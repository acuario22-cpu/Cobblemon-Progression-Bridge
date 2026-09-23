package dev.acuario22.cprog;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ProgressionBridge.MOD_ID)
public class ProgressionBridge {
    public static final String MOD_ID = "cobblemon_progression_bridge";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "cprog";
    public ProgressionBridge() {
        MinecraftForge.EVENT_BUS.register(this);
        CobblemonHooks.register();
        LOGGER.info("Cobblemon Progression Bridge iniciado.");
    }
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ScoreboardProgress.ensureAll(player);
    }
    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ScoreboardProgress.ensureAll(player);
        trackBiome(player);
        trackBloodMoonTransition(player);
    }
    private static CompoundTag persistent(ServerPlayer player) {
        CompoundTag forge = player.getPersistentData();
        if (!forge.contains(Player.PERSISTED_NBT_TAG)) forge.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = forge.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) persisted.put(ROOT, new CompoundTag());
        return persisted.getCompound(ROOT);
    }
    private static void trackBiome(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
            ResourceLocation id = key.location();
            String ns = id.getNamespace();
            if (!ns.equals("biomesoplenty") && !ns.equals("regions_unexplored")) return;
            CompoundTag data = persistent(player);
            String listKey = ns.equals("biomesoplenty") ? "visited_bop" : "visited_ru";
            ListTag list = data.getList(listKey, 8);
            String value = id.toString();
            for (int i = 0; i < list.size(); i++) if (value.equals(list.getString(i))) return;
            list.add(StringTag.valueOf(value));
            data.put(listKey, list);
            ScoreboardProgress.add(player, ns.equals("biomesoplenty") ? "cprog_bop_biomes" : "cprog_ru_biomes", 1);
        });
    }
    private static void trackBloodMoonTransition(ServerPlayer player) {
        if (!ModList.get().isLoaded("enhancedcelestials")) return;
        CompoundTag data = persistent(player);
        boolean now = ExternalIntegrations.isBloodMoon(player.serverLevel());
        boolean before = data.getBoolean("bloodmoon_active");
        if (now && !before) ScoreboardProgress.add(player, "cprog_bloodmoon_seen", 1);
        else if (!now && before) ScoreboardProgress.add(player, "cprog_bloodmoon_complete", 1);
        data.putBoolean("bloodmoon_active", now);
    }
    public static boolean rememberLegendary(ServerPlayer player, String speciesId) {
        CompoundTag data = persistent(player);
        ListTag list = data.getList("legendary_species", 8);
        for (int i = 0; i < list.size(); i++) if (speciesId.equals(list.getString(i))) return false;
        list.add(StringTag.valueOf(speciesId));
        data.put("legendary_species", list);
        return true;
    }
}
