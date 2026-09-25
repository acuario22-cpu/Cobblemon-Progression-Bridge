package dev.cobblemodern;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.cobblemodern.alpha.AlphaSizeSystem;
import dev.cobblemodern.command.HerdCommands;
import dev.cobblemodern.command.ModernCommands;
import dev.cobblemodern.config.ModernConfig;
import dev.cobblemodern.habitat.HabitatSystem;
import dev.cobblemodern.herd.HerdSystem;
import dev.cobblemodern.network.ModernNetwork;
import dev.cobblemodern.registry.ModBlocks;
import dev.cobblemodern.registry.ModItems;
import dev.cobblemodern.tm.TMSystem;
import kotlin.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModernBackport.MOD_ID)
public final class ModernBackport {
    public static final String MOD_ID = "cobblemon_modern_backport";

    public ModernBackport() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModernNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModernConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            PokemonEntity entity = event.getEntity();
            boolean alpha = AlphaSizeSystem.initializeNaturalSpawn(entity);
            HerdSystem.scheduleMaybeCreateHerd(entity, alpha);
            return Unit.INSTANCE;
        });
    }

    public static final class ForgeEvents {
        @SubscribeEvent
        public void onLivingTick(LivingEvent.LivingTickEvent event) {
            if (event.getEntity() instanceof PokemonEntity pokemon) HerdSystem.tickMember(pokemon);
        }

        @SubscribeEvent
        public void onChunkLoad(ChunkEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                var pos = event.getChunk().getPos();
                level.getServer().execute(() -> HabitatSystem.tryGenerateForChunk(level, pos));
            }
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.getServer() != null) HabitatSystem.tick(event.getServer());
        }

        @SubscribeEvent
        public void onClone(PlayerEvent.Clone event) {
            if (event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer oldPlayer
                && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer newPlayer) {
                TMSystem.copyPersistentData(oldPlayer, newPlayer);
            }
        }

        @SubscribeEvent
        public void onCommands(RegisterCommandsEvent event) {
            ModernCommands.register(event.getDispatcher());
            HerdCommands.register(event.getDispatcher());
        }
    }
}
