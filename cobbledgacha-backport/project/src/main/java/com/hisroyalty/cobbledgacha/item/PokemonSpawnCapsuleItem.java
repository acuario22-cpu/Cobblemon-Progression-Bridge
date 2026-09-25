package com.hisroyalty.cobbledgacha.item;

import com.hisroyalty.cobbledgacha.cobblemon.PokemonCommandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PokemonSpawnCapsuleItem extends Item {
    private final String pool;

    public PokemonSpawnCapsuleItem(String pool, Properties properties) {
        super(properties);
        this.pool = pool;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(held);
        }

        var result = PokemonCommandSpawner.spawn(
            (ServerLevel) level,
            player.blockPosition(),
            serverPlayer,
            true,
            pool
        );

        if (result.success()) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.displayClientMessage(
                Component.translatable(
                    "message.cobbledgacha.received_pokemon",
                    Component.literal(pretty(result.species())),
                    result.level()
                ).withStyle(ChatFormatting.AQUA),
                false
            );
        } else {
            player.displayClientMessage(
                Component.translatable("message.cobbledgacha.pokemon_failed")
                    .withStyle(ChatFormatting.RED),
                false
            );
        }

        level.playSound(
            null,
            player.blockPosition(),
            net.minecraft.sounds.SoundEvents.SNIFFER_EGG_PLOP,
            net.minecraft.sounds.SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
        return result.success()
            ? InteractionResultHolder.consume(held)
            : InteractionResultHolder.fail(held);
    }

    private static String pretty(String species) {
        String value = species.replace('_', ' ');
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
