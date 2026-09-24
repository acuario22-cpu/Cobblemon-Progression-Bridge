package dev.acuario22.tanrealism;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import toughasnails.api.temperature.IPlayerTemperatureModifier;
import toughasnails.api.temperature.TemperatureLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RealisticPokemonTemperatureModifier implements IPlayerTemperatureModifier {
    private record PokemonThermal(boolean wild, boolean owned, double distance, FormData form, int level) {
    }

    @Override
    public TemperatureLevel modify(Player player, TemperatureLevel temperatureLevel) {
        if (!ThermalConfig.ENABLE_POKEMON_TEMPERATURE.get()) {
            return temperatureLevel;
        }

        double searchRange = Math.max(
                ThermalConfig.MAX_RANGE.get(),
                ThermalConfig.FULL_STRENGTH_RADIUS.get()
        );

        List<PokemonThermal> pokemon = new ArrayList<>();

        for (PokemonEntity entity : player.level().getEntitiesOfClass(
                PokemonEntity.class,
                player.getBoundingBox().inflate(searchRange))) {
            pokemon.add(new PokemonThermal(
                    entity.getPokemon().isWild(),
                    entity.isOwnedBy(player),
                    player.distanceTo(entity),
                    entity.getForm(),
                    entity.getPokemon().getLevel()
            ));
        }

        for (Player nearbyPlayer : player.level().getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(searchRange))) {
            addShoulderPokemon(pokemon, player, nearbyPlayer, nearbyPlayer.getShoulderEntityLeft());
            addShoulderPokemon(pokemon, player, nearbyPlayer, nearbyPlayer.getShoulderEntityRight());
        }

        double harmfulDelta = 0.0;
        double safeDelta = 0.0;

        for (PokemonThermal entry : pokemon) {
            if (entry.wild && !ThermalConfig.WILD_AFFECTS_TEMPERATURE.get()) {
                continue;
            }
            if (!entry.wild && !ThermalConfig.CAPTURED_AFFECTS_TEMPERATURE.get()) {
                continue;
            }
            if (entry.form == null) {
                continue;
            }

            double range = Mth.clamp(
                    ThermalConfig.BASE_RANGE.get() + ThermalConfig.RANGE_PER_LEVEL.get() * entry.level,
                    0.0,
                    ThermalConfig.MAX_RANGE.get()
            );

            if (range <= 0.0 || entry.distance > range) {
                continue;
            }

            double strength = Mth.clamp(
                    ThermalConfig.BASE_STRENGTH.get() + ThermalConfig.STRENGTH_PER_LEVEL.get() * entry.level,
                    0.0,
                    ThermalConfig.MAX_STRENGTH.get()
            );

            double typeDelta = signedTypeStrength(entry.form, strength);
            if (Math.abs(typeDelta) < 1.0E-7) {
                continue;
            }

            double falloff = calculateDistanceFactor(entry.distance, range);
            double effectiveDelta = typeDelta * falloff;

            if (canCauseExtremes(entry)) {
                harmfulDelta += effectiveDelta;
            } else {
                safeDelta += effectiveDelta;
            }
        }

        double cap = ThermalConfig.MAX_COMBINED_EFFECT.get();
        harmfulDelta = Mth.clamp(harmfulDelta, -cap, cap);
        safeDelta = Mth.clamp(safeDelta, -cap, cap);

        double newTemperature = temperatureLevel.ordinal() - 2.0;

        // Pokemon allowed to be dangerous can reach the full TAN range (-2..+2).
        newTemperature = Mth.clamp(newTemperature + harmfulDelta, -2.0, 2.0);

        // Safe Pokemon can rescue the player from an opposite extreme, but cannot create HOT/ICY.
        if (safeDelta > 0.0) {
            if (newTemperature < 1.999) {
                newTemperature = Math.min(newTemperature + safeDelta, 1.0);
            }
        } else if (safeDelta < 0.0) {
            if (newTemperature > -1.999) {
                newTemperature = Math.max(newTemperature + safeDelta, -1.0);
            }
        }

        int index = Mth.clamp((int) Math.round(newTemperature) + 2, 0, TemperatureLevel.values().length - 1);
        return TemperatureLevel.values()[index];
    }

    private static double signedTypeStrength(FormData form, double strength) {
        double delta = 0.0;
        double secondaryStrength = strength * ThermalConfig.SECONDARY_TYPE_MULTIPLIER.get();

        if (Objects.equals(form.getPrimaryType(), ElementalTypes.INSTANCE.getFIRE())) {
            delta += strength;
        } else if (Objects.equals(form.getSecondaryType(), ElementalTypes.INSTANCE.getFIRE())) {
            delta += secondaryStrength;
        }

        if (Objects.equals(form.getPrimaryType(), ElementalTypes.INSTANCE.getICE())) {
            delta -= strength;
        } else if (Objects.equals(form.getSecondaryType(), ElementalTypes.INSTANCE.getICE())) {
            delta -= secondaryStrength;
        }

        return delta;
    }

    private static double calculateDistanceFactor(double distance, double range) {
        double fullRadius = Math.min(ThermalConfig.FULL_STRENGTH_RADIUS.get(), range);

        if (distance <= fullRadius) {
            return 1.0;
        }
        if (distance >= range) {
            return 0.0;
        }

        double denominator = range - fullRadius;
        if (denominator <= 1.0E-7) {
            return 0.0;
        }

        double t = Mth.clamp((distance - fullRadius) / denominator, 0.0, 1.0);

        return switch (ThermalConfig.FALLOFF_CURVE.get()) {
            case LINEAR -> 1.0 - t;
            case SMOOTHSTEP -> {
                double smooth = t * t * (3.0 - 2.0 * t);
                yield 1.0 - smooth;
            }
        };
    }

    private static boolean canCauseExtremes(PokemonThermal entry) {
        if (entry.wild) {
            return ThermalConfig.WILD_CAUSES_EXTREMES.get();
        }
        if (entry.owned) {
            return ThermalConfig.OWNED_CAUSES_EXTREMES.get();
        }
        return ThermalConfig.OTHER_CAPTURED_CAUSES_EXTREMES.get();
    }

    private static void addShoulderPokemon(List<PokemonThermal> pokemon,
                                           Player observingPlayer,
                                           Player shoulderOwner,
                                           CompoundTag shoulderTag) {
        if (shoulderTag == null || shoulderTag.isEmpty() || !shoulderTag.contains("Pokemon")) {
            return;
        }

        try {
            CompoundTag pokemonTag = shoulderTag.getCompound("Pokemon");
            ResourceLocation speciesId = new ResourceLocation(pokemonTag.getString("Species"));
            String formId = pokemonTag.getString("FormId");
            int level = Math.max(1, pokemonTag.getShort("Level"));

            var species = PokemonSpecies.INSTANCE.getByIdentifier(speciesId);
            if (species == null) {
                return;
            }

            FormData form = species.getForm(Collections.singleton(formId));
            double distance = shoulderOwner.is(observingPlayer) ? 0.0 : observingPlayer.distanceTo(shoulderOwner);

            pokemon.add(new PokemonThermal(
                    false,
                    shoulderOwner.is(observingPlayer),
                    distance,
                    form,
                    level
            ));
        } catch (Throwable ignored) {
            // Invalid/foreign shoulder data is ignored instead of breaking temperature calculation.
        }
    }
}
