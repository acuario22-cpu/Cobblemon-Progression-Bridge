package dev.acuario22.tanrealism;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ThermalConfig {
    public enum FalloffCurve {
        LINEAR,
        SMOOTHSTEP
    }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_POKEMON_TEMPERATURE;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ORIGINAL_CI_TEMPERATURE;

    public static final ForgeConfigSpec.BooleanValue WILD_AFFECTS_TEMPERATURE;
    public static final ForgeConfigSpec.BooleanValue CAPTURED_AFFECTS_TEMPERATURE;
    public static final ForgeConfigSpec.BooleanValue WILD_CAUSES_EXTREMES;
    public static final ForgeConfigSpec.BooleanValue OWNED_CAUSES_EXTREMES;
    public static final ForgeConfigSpec.BooleanValue OTHER_CAPTURED_CAUSES_EXTREMES;

    public static final ForgeConfigSpec.DoubleValue BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue RANGE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_RANGE;
    public static final ForgeConfigSpec.DoubleValue FULL_STRENGTH_RADIUS;
    public static final ForgeConfigSpec.EnumValue<FalloffCurve> FALLOFF_CURVE;

    public static final ForgeConfigSpec.DoubleValue BASE_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue STRENGTH_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SECONDARY_TYPE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MAX_COMBINED_EFFECT;

    public static final ForgeConfigSpec.BooleanValue MILD_WORLD_PRESET;
    public static final ForgeConfigSpec.IntValue AMBIENT_CHANGE_DELAY;
    public static final ForgeConfigSpec.IntValue POKEMON_CHANGE_DELAY;
    public static final ForgeConfigSpec.BooleanValue DISABLE_NIGHT_SHIFT;
    public static final ForgeConfigSpec.BooleanValue DISABLE_WEATHER_SHIFT;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ALTITUDE_SHIFT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("pokemon_temperature");
        ENABLE_POKEMON_TEMPERATURE = builder
                .comment("Enable realistic Fire/Ice Pokemon temperature effects.")
                .define("enabled", true);
        DISABLE_ORIGINAL_CI_TEMPERATURE = builder
                .comment("If Cobblemon Integrations is installed, disable only its original Pokemon temperature modifier to prevent double effects. Water filling and its other integrations remain enabled.")
                .define("disableOriginalCobblemonIntegrationsTemperature", true);

        builder.push("behavior");
        WILD_AFFECTS_TEMPERATURE = builder.define("wildAffectsTemperature", true);
        CAPTURED_AFFECTS_TEMPERATURE = builder.define("capturedAffectsTemperature", true);
        WILD_CAUSES_EXTREMES = builder
                .comment("Wild Pokemon may push the player into HOT/ICY.")
                .define("wildCausesExtremes", true);
        OWNED_CAUSES_EXTREMES = builder
                .comment("Your own Pokemon may push you into HOT/ICY. Recommended false.")
                .define("ownedCausesExtremes", false);
        OTHER_CAPTURED_CAUSES_EXTREMES = builder
                .comment("Other players' captured Pokemon may push you into HOT/ICY.")
                .define("otherCapturedCausesExtremes", false);
        builder.pop();

        builder.push("distance");
        BASE_RANGE = builder
                .comment("Base influence radius in blocks.")
                .defineInRange("baseRange", 1.5, 0.0, 64.0);
        RANGE_PER_LEVEL = builder
                .comment("Extra influence radius per Pokemon level.")
                .defineInRange("rangePerLevel", 0.035, 0.0, 1.0);
        MAX_RANGE = builder
                .comment("Maximum influence radius in blocks.")
                .defineInRange("maxRange", 5.0, 0.1, 64.0);
        FULL_STRENGTH_RADIUS = builder
                .comment("Inside this distance, the Pokemon applies 100% of its thermal strength.")
                .defineInRange("fullStrengthRadius", 0.75, 0.0, 16.0);
        FALLOFF_CURVE = builder
                .comment("SMOOTHSTEP gives a natural gradual falloff; LINEAR is mathematically straight.")
                .defineEnum("falloffCurve", FalloffCurve.SMOOTHSTEP);
        builder.pop();

        builder.push("strength");
        BASE_STRENGTH = builder
                .comment("Base thermal strength before level scaling.")
                .defineInRange("baseStrength", 0.42, 0.0, 4.0);
        STRENGTH_PER_LEVEL = builder
                .comment("Thermal strength gained per Pokemon level.")
                .defineInRange("strengthPerLevel", 0.017, 0.0, 1.0);
        MAX_STRENGTH = builder
                .comment("Maximum strength of one Pokemon at point-blank range.")
                .defineInRange("maxStrength", 2.12, 0.0, 4.0);
        SECONDARY_TYPE_MULTIPLIER = builder
                .comment("Multiplier when Fire/Ice is the secondary type. 1.0 means no arbitrary half-strength penalty.")
                .defineInRange("secondaryTypeMultiplier", 1.0, 0.0, 1.0);
        MAX_COMBINED_EFFECT = builder
                .comment("Maximum combined Pokemon contribution in either direction.")
                .defineInRange("maxCombinedEffect", 2.0, 0.0, 4.0);
        builder.pop();
        builder.pop();

        builder.push("mild_world");
        MILD_WORLD_PRESET = builder
                .comment("Apply the mild-world runtime preset. Built-in biome tags also downgrade Overworld HOT/ICY biomes to WARM/COLD.")
                .define("enabled", true);
        AMBIENT_CHANGE_DELAY = builder
                .comment("Base environmental temperature change delay in ticks. 2400 = 120 seconds.")
                .defineInRange("ambientChangeDelay", 2400, 0, Integer.MAX_VALUE);
        POKEMON_CHANGE_DELAY = builder
                .comment("Player-modifier delay used for Pokemon temperature response. 100 = 5 seconds.")
                .defineInRange("pokemonChangeDelay", 100, 0, Integer.MAX_VALUE);
        DISABLE_NIGHT_SHIFT = builder
                .comment("Prevent night alone from adding an extra hot/cold step.")
                .define("disableNightTemperatureShift", true);
        DISABLE_WEATHER_SHIFT = builder
                .comment("Prevent ordinary wetness/snowfall from adding an extra cold step. Powder snow remains dangerous.")
                .define("disableWeatherTemperatureShift", true);
        DISABLE_ALTITUDE_SHIFT = builder
                .comment("Nearly disable altitude-based ambient temperature shifts.")
                .define("disableAltitudeTemperatureShift", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ThermalConfig() {
    }
}
