package dev.zanckor.cobblemonrider;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.zanckor.cobblemonrider.config.PokemonJsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import static dev.zanckor.cobblemonrider.config.PokemonJsonObject.MountType.FLY;

public final class MountSafety {
    public static final String FALL_PROTECTION_TAG = "cobblemonrider_safe_fall_ticks";
    private static final int MAX_PROTECTION_TICKS = 600;

    private MountSafety() {
    }

    public static void prepare(Player player) {
        if (player == null) return;

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof PokemonEntity pokemon)) return;

        PokemonJsonObject.PokemonConfigData config = FormRideCompat.resolve(
                pokemon.getPokemon().getSpecies().getName(),
                pokemon.getPokemon().getForm().getName()
        );

        if (config == null || !config.getMountTypes().contains(FLY)) return;
        if (player.onGround() || player.isInWater() || player.isInLava()) return;

        player.fallDistance = 0.0F;
        player.getPersistentData().putInt(FALL_PROTECTION_TAG, MAX_PROTECTION_TICKS);
    }

    public static void tick(Player player) {
        CompoundTag tag = player.getPersistentData();
        int remaining = tag.getInt(FALL_PROTECTION_TAG);
        if (remaining <= 0) return;

        player.fallDistance = 0.0F;

        if (player.onGround() || player.isInWater() || player.isInLava()) {
            tag.remove(FALL_PROTECTION_TAG);
            return;
        }

        if (remaining <= 1) {
            tag.remove(FALL_PROTECTION_TAG);
        } else {
            tag.putInt(FALL_PROTECTION_TAG, remaining - 1);
        }
    }

    public static boolean consumeFallProtection(Player player) {
        CompoundTag tag = player.getPersistentData();
        if (tag.getInt(FALL_PROTECTION_TAG) <= 0) {
            return false;
        }

        player.fallDistance = 0.0F;
        tag.remove(FALL_PROTECTION_TAG);
        return true;
    }
}
