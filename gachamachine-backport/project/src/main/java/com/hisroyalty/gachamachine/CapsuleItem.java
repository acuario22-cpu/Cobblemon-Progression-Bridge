package com.hisroyalty.gachamachine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class CapsuleItem extends Item {
    public CapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(held);

        ServerLevel serverLevel = (ServerLevel) level;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(this);
        if (itemId == null) return InteractionResultHolder.fail(held);

        ResourceLocation lootId = new ResourceLocation(itemId.getNamespace(), "gacha_capsules/" + itemId.getPath());
        LootTable table = serverLevel.getServer().getLootData().getLootTable(lootId);
        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
            .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = table.getRandomItems(params);
        if (drops.isEmpty()) {
            player.displayClientMessage(
                Component.translatable("message.capsule.empty_loot_table").withStyle(ChatFormatting.RED), true);
        } else {
            for (ItemStack drop : drops) {
                if (!player.addItem(drop)) player.drop(drop, false);
            }
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        level.playSound(null, player.blockPosition(),
            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(held);
    }
}
