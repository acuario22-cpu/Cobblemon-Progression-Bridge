package dev.cobblemodern.command;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.cobblemodern.alpha.AlphaSizeSystem;
import dev.cobblemodern.herd.HerdSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HerdCommands {
    private static final double TARGET_RADIUS = 12.0;
    private static final double SCAN_RADIUS = 48.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cmodern")
                .then(Commands.literal("herd")
                    .then(Commands.literal("info")
                        .executes(HerdCommands::info)))
        );
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();

            PokemonEntity target = level.getEntitiesOfClass(
                    PokemonEntity.class,
                    new AABB(player.blockPosition()).inflate(TARGET_RADIUS),
                    p -> p.isAlive()
                )
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);

            if (target == null) {
                player.sendSystemMessage(
                    Component.literal("No hay Pokémon cerca (radio de 12 bloques).")
                        .withStyle(ChatFormatting.GRAY)
                );
                return 0;
            }

            CompoundTag data = target.getPokemon().getPersistentData();
            String herdId = data.getString(HerdSystem.KEY_HERD_ID);
            String pokemonName = target.getPokemon().getDisplayName().getString();
            boolean alpha = AlphaSizeSystem.isAlpha(target.getPokemon());
            float scale = target.getPokemon().getScaleModifier();

            player.sendSystemMessage(
                Component.literal("=== Diagnóstico de manada ===")
                    .withStyle(ChatFormatting.GOLD)
            );

            player.sendSystemMessage(
                Component.literal(
                    pokemonName + " | Alpha: " + (alpha ? "Sí" : "No")
                        + " | Tamaño: " + String.format(Locale.ROOT, "%.2fx", scale)
                ).withStyle(alpha ? ChatFormatting.GOLD : ChatFormatting.AQUA)
            );

            if (herdId == null || herdId.isBlank()) {
                player.sendSystemMessage(
                    Component.literal("Manada CModern: No")
                        .withStyle(ChatFormatting.RED)
                );
                player.sendSystemMessage(
                    Component.literal("Rol: — | ID de manada: —")
                        .withStyle(ChatFormatting.GRAY)
                );
                player.sendSystemMessage(
                    Component.literal(
                        "Este Pokémon no tiene las marcas internas de una manada CModern. "
                            + "Si está rodeado por muchos Pokémon, puede ser un Outbreak u otro sistema."
                    ).withStyle(ChatFormatting.YELLOW)
                );
                return 1;
            }

            boolean leader = data.getBoolean(HerdSystem.KEY_LEADER);
            String leaderUuidText = leader
                ? target.getUUID().toString()
                : data.getString(HerdSystem.KEY_LEADER_UUID);

            List<PokemonEntity> sameHerd = level.getEntitiesOfClass(
                PokemonEntity.class,
                target.getBoundingBox().inflate(SCAN_RADIUS),
                p -> herdId.equals(p.getPokemon().getPersistentData().getString(HerdSystem.KEY_HERD_ID))
            );

            int leaders = 0;
            int followers = 0;
            for (PokemonEntity p : sameHerd) {
                if (p.getPokemon().getPersistentData().getBoolean(HerdSystem.KEY_LEADER)) leaders++;
                else followers++;
            }

            player.sendSystemMessage(
                Component.literal("Manada CModern: Sí")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("  (marca independiente de Outbreak)").withStyle(ChatFormatting.GRAY))
            );

            player.sendSystemMessage(
                Component.literal("Rol: " + (leader ? "Líder" : "Miembro"))
                    .withStyle(leader ? ChatFormatting.GOLD : ChatFormatting.AQUA)
            );

            player.sendSystemMessage(
                Component.literal("ID de manada: " + herdId)
                    .withStyle(ChatFormatting.DARK_AQUA)
            );

            player.sendSystemMessage(
                Component.literal(
                    "Cargados en 48 bloques: " + sameHerd.size()
                        + " total | " + leaders + " líder | " + followers + " miembros"
                ).withStyle(ChatFormatting.WHITE)
            );

            if (leader) {
                player.sendSystemMessage(
                    Component.literal("Líder UUID: " + target.getUUID())
                        .withStyle(ChatFormatting.GRAY)
                );
            } else if (leaderUuidText != null && !leaderUuidText.isBlank()) {
                try {
                    UUID leaderUuid = UUID.fromString(leaderUuidText);
                    Entity entity = level.getEntity(leaderUuid);

                    if (entity instanceof PokemonEntity leaderEntity && leaderEntity.isAlive()) {
                        double distance = Math.sqrt(target.distanceToSqr(leaderEntity));
                        player.sendSystemMessage(
                            Component.literal(
                                "Líder: " + leaderEntity.getPokemon().getDisplayName().getString()
                                    + " | Distancia: "
                                    + String.format(Locale.ROOT, "%.1f", distance) + " bloques"
                            ).withStyle(ChatFormatting.YELLOW)
                        );
                        player.sendSystemMessage(
                            Component.literal("Líder UUID: " + leaderUuidText)
                                .withStyle(ChatFormatting.GRAY)
                        );
                    } else {
                        player.sendSystemMessage(
                            Component.literal("Líder: fuera de chunks cargados o ya no existe.")
                                .withStyle(ChatFormatting.YELLOW)
                        );
                        player.sendSystemMessage(
                            Component.literal("Líder UUID guardado: " + leaderUuidText)
                                .withStyle(ChatFormatting.GRAY)
                        );
                    }
                } catch (IllegalArgumentException ex) {
                    player.sendSystemMessage(
                        Component.literal("Líder UUID guardado: inválido")
                            .withStyle(ChatFormatting.RED)
                    );
                }
            } else {
                player.sendSystemMessage(
                    Component.literal("Aviso: miembro con ID de manada pero sin UUID de líder.")
                        .withStyle(ChatFormatting.RED)
                );
            }

            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }

    private HerdCommands() {}
}
