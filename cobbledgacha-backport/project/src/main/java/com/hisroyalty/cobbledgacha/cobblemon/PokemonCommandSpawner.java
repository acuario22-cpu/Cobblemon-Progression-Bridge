package com.hisroyalty.cobbledgacha.cobblemon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public final class PokemonCommandSpawner {
    private PokemonCommandSpawner() {}

    public record SpawnResult(boolean success, String species, int level) {
        public static SpawnResult failed() {
            return new SpawnResult(false, "", 0);
        }
    }

    public static SpawnResult spawn(ServerLevel level, BlockPos pos, ServerPlayer player, boolean give, String pool) {
        var entry = GachaSpawnPools.roll(pool, new Random(level.random.nextLong()));
        if (entry == null) return SpawnResult.failed();

        int pokemonLevel = entry.minLevel();
        if (entry.maxLevel() > entry.minLevel()) {
            pokemonLevel += level.random.nextInt(entry.maxLevel() - entry.minLevel() + 1);
        }

        String properties = entry.species() + " level=" + pokemonLevel;
        try {
            var source = level.getServer().createCommandSourceStack()
                .withPermission(4)
                .withLevel(level)
                .withPosition(Vec3.atCenterOf(pos));

            int result = give && player != null
                ? level.getServer().getCommands().performPrefixedCommand(
                    source, "givepokemonother " + player.getGameProfile().getName() + " " + properties)
                : level.getServer().getCommands().performPrefixedCommand(
                    source, "spawnpokemonat " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + properties);

            return result > 0
                ? new SpawnResult(true, entry.species(), pokemonLevel)
                : SpawnResult.failed();
        } catch (Exception ex) {
            return SpawnResult.failed();
        }
    }
}
