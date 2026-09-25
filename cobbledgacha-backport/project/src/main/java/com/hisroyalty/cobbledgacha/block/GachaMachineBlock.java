package com.hisroyalty.cobbledgacha.block;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import com.hisroyalty.cobbledgacha.cobblemon.PokemonCommandSpawner;
import com.hisroyalty.cobbledgacha.config.DatapackConfig;
import com.hisroyalty.cobbledgacha.config.MachineType;
import com.hisroyalty.cobbledgacha.world.GachaCooldownData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class GachaMachineBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final UUID AUTOMATION_UUID = UUID.nameUUIDFromBytes("cobbledgacha".getBytes(StandardCharsets.UTF_8));

    private final TagKey<Item> currencyTag;
    private final String lootKey;
    private final int configProperty;

    public GachaMachineBlock(Properties properties, TagKey<Item> currencyTag, String lootKey, int configProperty) {
        super(properties);
        this.currencyTag = currencyTag;
        this.lootKey = lootKey;
        this.configProperty = configProperty;
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public TagKey<Item> getCurrencyTag() { return currencyTag; }
    public String getLootKey() { return lootKey; }
    public String getMachineKey() { return "gacha_machine_" + configProperty; }
    public MachineType getMachineType() { return DatapackConfig.type(getMachineKey()); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1
            || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    private BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockPos low = lower(state, pos);
            BlockEntity blockEntity = level.getBlockEntity(low);
            if (blockEntity instanceof GachaMachineBlockEntity machine) {
                Containers.dropContents(level, low, machine);
            }

            BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
                level.removeBlock(other, false);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
            ? RenderShape.ENTITYBLOCK_ANIMATED
            : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
            ? new GachaMachineBlockEntity(pos, state)
            : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return null;
        return createTickerHelper(type, CobbledGacha.GACHA_MACHINE_BE.get(), GachaMachineBlockEntity::tick);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos low = lower(state, pos);
        BlockEntity raw = level.getBlockEntity(low);
        if (!(raw instanceof GachaMachineBlockEntity machine)) return InteractionResult.PASS;

        boolean ok = processCurrency(
            (ServerLevel) level,
            low,
            machine,
            player.getItemInHand(hand),
            player instanceof ServerPlayer serverPlayer ? serverPlayer : null,
            false
        );
        return ok ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    public boolean processCurrency(
        ServerLevel level,
        BlockPos pos,
        GachaMachineBlockEntity machine,
        ItemStack currency,
        @Nullable ServerPlayer player,
        boolean automation
    ) {
        if (currency.isEmpty() || !currency.is(currencyTag)) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable(
                        "message.cobbledgacha.invalid_currency_detail",
                        requiredCurrencyDescription()
                    ).withStyle(ChatFormatting.RED),
                    true
                );
            }
            return false;
        }

        ResourceLocation currencyId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(currency.getItem());
        if (getMachineType() == MachineType.SPECIFIC
            && machine.getLockedCurrencyId() != null
            && !machine.getLockedCurrencyId().equals(currencyId)) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.cobbledgacha.same_yarn_required")
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
            return false;
        }

        UUID cooldownId = player == null ? AUTOMATION_UUID : player.getUUID();
        GachaCooldownData cooldowns = GachaCooldownData.get(level);
        long remaining = cooldowns.remaining(level, getMachineKey(), cooldownId);
        if (remaining > 0) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable(
                        "message.gacha_machine.cooldown",
                        Component.literal(Long.toString((remaining + 19) / 20))
                    ).withStyle(ChatFormatting.RED),
                    true
                );
            }
            return false;
        }

        if (getMachineType() == MachineType.SPECIFIC && machine.getLockedCurrencyId() == null) {
            machine.setLockedCurrencyId(currencyId);
        }

        if (player == null || !player.getAbilities().instabuild) {
            currency.shrink(1);
        }

        int next = machine.getGachaLevel() + 1;
        int max = DatapackConfig.maxCurrency(getMachineKey());

        level.playSound(
            null,
            pos,
            net.minecraft.sounds.SoundEvents.CHAIN_STEP,
            net.minecraft.sounds.SoundSource.BLOCKS,
            0.75F,
            1.0F
        );

        if (next < max) {
            machine.setGachaLevel(next);
            if (player != null) {
                player.displayClientMessage(Component.literal("[" + next + "/" + max + "]"), true);
            }
            return true;
        }

        // Always show the real final count before resetting.
        if (player != null) {
            player.displayClientMessage(
                Component.literal("[" + max + "/" + max + "]").withStyle(ChatFormatting.GREEN),
                true
            );
        }

        machine.setGachaLevel(0);
        machine.playDispenseAnimation();

        boolean dispensed;
        if (getMachineType() == MachineType.SPAWNER) {
            Direction facing = machine.getBlockState().getValue(FACING);
            var result = PokemonCommandSpawner.spawn(
                level,
                pos.relative(facing),
                player,
                false,
                lootKey
            );
            dispensed = result.success();

            if (player != null) {
                if (result.success()) {
                    player.displayClientMessage(
                        Component.translatable(
                            "message.cobbledgacha.spawned_pokemon",
                            Component.literal(pretty(result.species())),
                            result.level()
                        ).withStyle(ChatFormatting.AQUA),
                        false
                    );
                } else {
                    player.displayClientMessage(
                        Component.translatable("message.cobbledgacha.reward_failed")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                }
            }
        } else {
            String tableKey = lootKey;
            if (getMachineType() == MachineType.SPECIFIC && machine.getLockedCurrencyId() != null) {
                tableKey += "_" + machine.getLockedCurrencyId().getPath();
            }

            List<ItemStack> rewards = roll(level, pos, player, tableKey);
            dispensed = !rewards.isEmpty();

            if (rewards.isEmpty()) {
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.cobbledgacha.reward_failed")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                }
            } else {
                for (ItemStack reward : rewards) {
                    deliver(level, pos, machine, player, reward);
                    if (player != null) {
                        player.displayClientMessage(
                            Component.translatable(
                                "message.cobbledgacha.obtained_item",
                                reward.getCount(),
                                reward.getHoverName()
                            ).withStyle(ChatFormatting.GOLD),
                            false
                        );
                    }
                }
            }
        }

        machine.clearLockedCurrency();
        cooldowns.recordUse(
            level,
            getMachineKey(),
            cooldownId,
            DatapackConfig.usesBeforeCooldown(getMachineKey()),
            DatapackConfig.cooldownSeconds(getMachineKey()) * 20
        );

        return dispensed || automation;
    }

    private Component requiredCurrencyDescription() {
        return switch (configProperty) {
            case 1, 5, 6, 7, 8, 9, 10 ->
                Component.translatable("message.cobbledgacha.currency.gacha_coin");
            case 2 ->
                Component.translatable("message.cobbledgacha.currency.apricorn");
            case 3 ->
                Component.translatable("message.cobbledgacha.currency.relic_coin");
            case 4 ->
                Component.translatable("message.cobbledgacha.currency.diamond");
            case 11 ->
                Component.translatable("message.cobbledgacha.currency.koban_coin");
            case 12 ->
                Component.translatable("message.cobbledgacha.currency.yarn");
            default ->
                Component.translatable("message.cobbledgacha.currency.valid");
        };
    }

    private List<ItemStack> roll(
        ServerLevel level,
        BlockPos pos,
        @Nullable ServerPlayer player,
        String tableKey
    ) {
        LootTable table = level.getServer().getLootData().getLootTable(CobbledGacha.id(tableKey));
        LootParams.Builder builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, pos.getCenter());
        if (player != null) {
            builder.withOptionalParameter(LootContextParams.THIS_ENTITY, player);
        }
        return table.getRandomItems(builder.create(LootContextParamSets.CHEST));
    }

    private void deliver(
        ServerLevel level,
        BlockPos pos,
        GachaMachineBlockEntity machine,
        @Nullable ServerPlayer player,
        ItemStack input
    ) {
        ItemStack remaining = input.copy();

        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof Container container) {
            remaining = insert(container, remaining);
        }

        if (!remaining.isEmpty() && DatapackConfig.pickup() && player != null && player.addItem(remaining)) {
            remaining = ItemStack.EMPTY;
        }

        if (!remaining.isEmpty()) {
            Direction facing = machine.getBlockState().getValue(FACING);
            BlockPos output = pos.relative(facing);
            Containers.dropItemStack(
                level,
                output.getX() + 0.5,
                output.getY() + 0.5,
                output.getZ() + 0.5,
                remaining
            );
        }
    }

    private ItemStack insert(Container container, ItemStack input) {
        ItemStack remaining = input.copy();

        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (existing.isEmpty() && container.canPlaceItem(slot, remaining)) {
                container.setItem(slot, remaining.copy());
                return ItemStack.EMPTY;
            }

            if (ItemStack.isSameItemSameTags(existing, remaining)
                && existing.getCount() < existing.getMaxStackSize()
                && container.canPlaceItem(slot, remaining)) {
                int move = Math.min(
                    remaining.getCount(),
                    existing.getMaxStackSize() - existing.getCount()
                );
                existing.grow(move);
                remaining.shrink(move);
                container.setChanged();
            }
        }

        return remaining;
    }

    private static String pretty(String species) {
        String value = species.replace('_', ' ');
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(lower(state, pos));
        return blockEntity instanceof GachaMachineBlockEntity machine
            ? Math.min(15, machine.getGachaLevel())
            : 0;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(lower(state, pos));
        return blockEntity instanceof WorldlyContainer container ? container : null;
    }
}
