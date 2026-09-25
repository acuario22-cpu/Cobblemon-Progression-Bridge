package com.hisroyalty.gachamachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GachaMachineBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int DEFAULT_MAX_CURRENCY = 5;

    private final TagKey<Item> currencyTag;
    private final String lootKey;
    private final String expectedCoinName;

    public GachaMachineBlock(Properties properties, TagKey<Item> currencyTag, String lootKey) {
        super(properties);
        this.currencyTag = currencyTag;
        this.lootKey = lootKey;
        this.expectedCoinName = expectedCoinName;
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public TagKey<Item> getCurrencyTag() { return currencyTag; }
    public String getLootKey() { return lootKey; }
    public String getExpectedCoinName() { return expectedCoinName; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() >= ctx.getLevel().getMaxBuildHeight() - 1
            || !ctx.getLevel().getBlockState(pos.above()).canBeReplaced(ctx)) return null;
        return defaultBlockState()
            .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
            .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    private BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean upper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        Direction facing = state.getValue(FACING);
        if (facing == Direction.EAST || facing == Direction.WEST) {
            return upper ? Block.box(0.5, 0, 1, 15.5, 16, 15) : Block.box(0, 0, 0.5, 15, 16, 15.5);
        }
        return upper ? Block.box(1, 0, 0.5, 15, 16, 15.5) : Block.box(0.5, 0, 0, 15.5, 16, 15);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockPos lower = lowerPos(state, pos);
            BlockEntity be = level.getBlockEntity(lower);
            if (be instanceof GachaMachineBlockEntity machine) Containers.dropContents(level, lower, machine);

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
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
            ? new GachaMachineBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return null;
        return createTickerHelper(type, GachaMachine.MACHINE_BE.get(), GachaMachineBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos lower = lowerPos(state, pos);
        BlockEntity raw = level.getBlockEntity(lower);
        if (!(raw instanceof GachaMachineBlockEntity be)) return InteractionResult.PASS;

        if (be.getCooldown() > 0) {
            player.displayClientMessage(
                Component.translatable("message.gacha_machine.cooldown", (be.getCooldown() + 19) / 20)
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !held.is(currencyTag)) {
            player.displayClientMessage(
                Component.translatable("message.gacha_machine.invalid_any_currency")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        int next = be.getGachaLevel() + 1;
        if (!player.getAbilities().instabuild) held.shrink(1);

        if (next < DEFAULT_MAX_CURRENCY) {
            be.setGachaLevel(next);
            player.displayClientMessage(Component.literal("[" + next + "/" + DEFAULT_MAX_CURRENCY + "]"), true);
        } else {
            player.displayClientMessage(Component.literal("[" + DEFAULT_MAX_CURRENCY + "/" + DEFAULT_MAX_CURRENCY + "]")
                .withStyle(ChatFormatting.GREEN), true);
            be.setGachaLevel(0);

            ItemStack reward = rollReward((ServerLevel) level, lower, player);
            if (reward.isEmpty()) {
                player.displayClientMessage(
                    Component.translatable("message.gacha_machine.dud").withStyle(ChatFormatting.RED), true);
            } else if (!player.addItem(reward)) {
                player.drop(reward, false);
            }
            level.playSound(null, lower,
                net.minecraft.sounds.SoundEvents.DISPENSER_DISPENSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    public ItemStack rollReward(ServerLevel level, BlockPos pos, @Nullable Player player) {
        ResourceLocation id = GachaMachine.id(lootKey);
        LootTable table = level.getServer().getLootData().getLootTable(id);
        LootParams.Builder builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, pos.getCenter());
        if (player != null) builder.withOptionalParameter(LootContextParams.THIS_ENTITY, player);

        List<ItemStack> drops = table.getRandomItems(builder.create(LootContextParamSets.CHEST));
        if (drops.isEmpty()) return ItemStack.EMPTY;

        ItemStack first = drops.get(0).copy();
        for (int i = 1; i < drops.size(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drops.get(i));
        }
        return first;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(lowerPos(state, pos));
        return be instanceof WorldlyContainer c ? c : null;
    }
}
