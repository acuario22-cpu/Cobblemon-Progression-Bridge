package dev.acuario22.cprog;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import java.util.UUID;

public final class CobblemonHooks {
    private static boolean registered;
    private CobblemonHooks() {}
    public static void register() {
        if (registered) return;
        registered = true;
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, event -> {
            ServerPlayer player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            ServerLevel level = player.serverLevel();
            UUID outbreakOwner = ExternalIntegrations.outbreakOwnerForPokemon(level, pokemon.getUuid());
            if (outbreakOwner != null) {
                ScoreboardProgress.add(player, "cprog_outbreak_capture", 1);
                if (pokemon.getShiny()) ScoreboardProgress.add(player, "cprog_outbreak_shiny", 1);
            }
            if (ModList.get().isLoaded("enhancedcelestials") && ExternalIntegrations.isBloodMoon(level)) {
                ScoreboardProgress.add(player, "cprog_bloodmoon_capture", 1);
                if (pokemon.getShiny()) ScoreboardProgress.add(player, "cprog_bloodmoon_shiny", 1);
            }
            switch (ExternalIntegrations.season(level)) {
                case "spring" -> ScoreboardProgress.add(player, "cprog_spring_capture", 1);
                case "summer" -> ScoreboardProgress.add(player, "cprog_summer_capture", 1);
                case "autumn" -> ScoreboardProgress.add(player, "cprog_autumn_capture", 1);
                case "winter" -> ScoreboardProgress.add(player, "cprog_winter_capture", 1);
            }
            if (pokemon.isLegendary() || pokemon.isMythical()) {
                String species = pokemon.getSpecies().getResourceIdentifier().toString();
                if (ProgressionBridge.rememberLegendary(player, species)) ScoreboardProgress.add(player, "cprog_legendary_unique", 1);
            }
            return Unit.INSTANCE;
        });
        CobblemonEvents.POKEMON_FAINTED.subscribe(Priority.HIGHEST, event -> {
            Pokemon pokemon = event.getPokemon();
            if (pokemon == null || pokemon.getOwnerUUID() != null) return Unit.INSTANCE;
            for (ServerLevel level : ProgressionServerLevels.levels()) {
                UUID ownerUuid = ExternalIntegrations.outbreakOwnerForPokemon(level, pokemon.getUuid());
                if (ownerUuid != null) {
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
                    if (owner != null) ScoreboardProgress.add(owner, "cprog_outbreak_defeat", 1);
                    break;
                }
            }
            return Unit.INSTANCE;
        });
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, event -> {
            for (ServerPlayer player : event.getBattle().getPlayers()) {
                if (ExternalIntegrations.isBloodMoon(player.serverLevel())) ScoreboardProgress.add(player, "cprog_bloodmoon_defeat", 1);
            }
            return Unit.INSTANCE;
        });
    }
}
