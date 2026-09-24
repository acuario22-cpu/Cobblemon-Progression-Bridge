package dev.acuario22.tanrealism;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import toughasnails.api.temperature.TemperatureHelper;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod(CobblemonTanRealism.MOD_ID)
public final class CobblemonTanRealism {
    public static final String MOD_ID = "cobblemon_tan_realism";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean MODIFIER_REGISTERED = new AtomicBoolean(false);

    public CobblemonTanRealism() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ThermalConfig.SPEC, "cobblemon-tan-realism.toml");
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onServerStarting(ServerStartingEvent event) {
        if (ThermalConfig.MILD_WORLD_PRESET.get()) {
            applyMildToughAsNailsProfile();
        }

        if (ThermalConfig.DISABLE_ORIGINAL_CI_TEMPERATURE.get()) {
            disableCobblemonIntegrationsTemperature();
        }

        if (ThermalConfig.ENABLE_POKEMON_TEMPERATURE.get() && MODIFIER_REGISTERED.compareAndSet(false, true)) {
            TemperatureHelper.registerPlayerTemperatureModifier(new RealisticPokemonTemperatureModifier());
            LOGGER.info("[Cobblemon TAN Realism] Registered realistic Pokemon temperature modifier.");
        }
    }

    private static void applyMildToughAsNailsProfile() {
        try {
            var config = toughasnails.init.ModConfig.temperature;
            if (config == null) {
                LOGGER.warn("[Cobblemon TAN Realism] Tough As Nails temperature config is not initialized yet.");
                return;
            }

            config.temperatureChangeDelay = ThermalConfig.AMBIENT_CHANGE_DELAY.get();
            config.playerTemperatureChangeDelay = ThermalConfig.POKEMON_CHANGE_DELAY.get();

            if (ThermalConfig.DISABLE_NIGHT_SHIFT.get()) {
                config.nightTemperatureChange = 0;
                config.nightHotTemperatureChange = 0;
            }

            if (ThermalConfig.DISABLE_WEATHER_SHIFT.get()) {
                config.wetTemperatureChange = 0;
                config.snowTemperatureChange = 0;
            }

            if (ThermalConfig.DISABLE_ALTITUDE_SHIFT.get()) {
                config.temperatureDropAltitude = 1024;
                config.temperatureRiseAltitude = -64;
                config.environmentalModifierAltitude = 256;
            }

            LOGGER.info("[Cobblemon TAN Realism] Applied mild Tough As Nails world profile.");
        } catch (Throwable t) {
            LOGGER.error("[Cobblemon TAN Realism] Could not apply Tough As Nails mild-world profile.", t);
        }
    }

    private static void disableCobblemonIntegrationsTemperature() {
        if (!ModList.get().isLoaded("cobblemonintegrations")) {
            return;
        }

        try {
            Class<?> integrationClass = Class.forName("com.arcaryx.cobblemonintegrations.forge.CobblemonIntegrationsForge");
            Field configField = integrationClass.getField("CONFIG");
            Object integrationConfig = configField.get(null);
            Field temperatureToggleField = integrationConfig.getClass().getField("pokemonAffectsTemperature");
            Object value = temperatureToggleField.get(integrationConfig);

            if (value instanceof ForgeConfigSpec.BooleanValue booleanValue) {
                booleanValue.set(false);
                LOGGER.info("[Cobblemon TAN Realism] Disabled Cobblemon Integrations' original Pokemon temperature modifier; hydration remains untouched.");
            } else {
                LOGGER.warn("[Cobblemon TAN Realism] Found Cobblemon Integrations, but its temperature toggle had an unexpected type.");
            }
        } catch (Throwable t) {
            LOGGER.error("[Cobblemon TAN Realism] Could not disable the original Cobblemon Integrations temperature modifier. Set toughasnails.temperature.pokemonAffectsTemperature=false in cobblemonintegrations config to avoid double effects.", t);
        }
    }
}
