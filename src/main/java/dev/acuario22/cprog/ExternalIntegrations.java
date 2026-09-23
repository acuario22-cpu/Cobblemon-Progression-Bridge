package dev.acuario22.cprog;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ExternalIntegrations {
    private ExternalIntegrations() {}
    public static UUID outbreakOwnerForPokemon(ServerLevel level, UUID pokemonUuid) {
        try {
            Class<?> pcm = Class.forName("com.scouter.cobbleoutbreaks.data.PokemonOutbreakManager");
            Object pm = pcm.getMethod("get", Level.class).invoke(null, level);
            if (!(boolean) pcm.getMethod("containsUUID", UUID.class).invoke(pm, pokemonUuid)) return null;
            UUID outbreakUuid = (UUID) pcm.getMethod("getOwnerUUID", UUID.class).invoke(pm, pokemonUuid);
            if (outbreakUuid == null) return null;
            Class<?> omc = Class.forName("com.scouter.cobbleoutbreaks.data.OutbreakManager");
            Object om = omc.getMethod("get", Level.class).invoke(null, level);
            Object portal = omc.getMethod("getOutbreakEntity", UUID.class).invoke(om, outbreakUuid);
            return portal == null ? null : (UUID) portal.getClass().getMethod("getOwnerUUID").invoke(portal);
        } catch (Throwable ignored) { return null; }
    }
    public static boolean isBloodMoon(ServerLevel level) {
        try {
            Class<?> ec = Class.forName("dev.corgitaco.enhancedcelestials.EnhancedCelestials");
            Object optional = ec.getMethod("lunarForecastWorldData", Level.class).invoke(null, level);
            if (!(optional instanceof Optional<?> opt) || opt.isEmpty()) return false;
            Object data = opt.get();
            Object holder = data.getClass().getMethod("currentLunarEventHolder").invoke(data);
            Object keyOptional = holder.getClass().getMethod("unwrapKey").invoke(holder);
            if (!(keyOptional instanceof Optional<?> keyOpt) || keyOpt.isEmpty()) return false;
            Object key = keyOpt.get();
            Object location = key.getClass().getMethod("location").invoke(key);
            String id = String.valueOf(location).toLowerCase(Locale.ROOT);
            return id.endsWith(":blood_moon") || id.endsWith(":super_blood_moon");
        } catch (Throwable ignored) { return false; }
    }
    public static String season(ServerLevel level) {
        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            Method getSeasonState = helper.getMethod("getSeasonState", Level.class);
            Object state = getSeasonState.invoke(null, level);
            return String.valueOf(state.getClass().getMethod("getSeason").invoke(state)).toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) { return ""; }
    }
}
