package dev.zanckor.cobblemonrider;

import dev.zanckor.cobblemonrider.config.PokemonJsonObject;

import java.util.Locale;
import java.util.Map;

/**
 * Form-aware mount lookup for Cobblemon, Megamons/Generations and other form addons.
 *
 * Lookup order:
 * 1) exact species + exact form from pokemonRideConfig.json
 * 2) transformed-species alias + exact form
 * 3) base entry of the real species
 * 4) base entry of the transformed-species alias
 *
 * This means explicit form-specific config always wins, while unknown Mega/GMAX/
 * Dynamax/Primal/other forms remain rideable by safely falling back to the base
 * species instead of becoming unmountable.
 */
public final class FormRideCompat {
    private static final Map<String, String> SPECIAL_SPECIES_ALIASES = Map.of(
            "megacharizardx", "charizard",
            "megacharizardy", "charizard",
            "megamewtwox", "mewtwo",
            "megamewtwoy", "mewtwo"
    );

    private static final String[] TRANSFORM_PREFIXES = {
            "gigantamax",
            "dynamax",
            "primal",
            "gmax",
            "mega"
    };

    private FormRideCompat() {
    }

    public static PokemonJsonObject.PokemonConfigData resolve(String speciesName, String formName) {
        if (speciesName == null || speciesName.isBlank()) {
            return null;
        }

        String normalizedForm = normalizeForm(formName);

        // Explicit form-specific entry has highest priority.
        PokemonJsonObject.PokemonConfigData exact =
                MCUtil.getPassengerObject(speciesName, normalizedForm);
        if (exact != null) {
            return exact;
        }

        String alias = resolveSpeciesAlias(speciesName);

        if (!alias.equalsIgnoreCase(speciesName)) {
            PokemonJsonObject.PokemonConfigData aliasExact =
                    MCUtil.getPassengerObject(alias, normalizedForm);
            if (aliasExact != null) {
                return aliasExact;
            }
        }

        // Generic fallback is intentionally form-agnostic. This covers Mega,
        // GMAX, Dynamax, Primal, Origin and future addon forms without a hard
        // dependency on the addon that created the form.
        PokemonJsonObject.PokemonConfigData base =
                MCUtil.getPassengerObject(speciesName, "none");
        if (base != null) {
            return base;
        }

        if (!alias.equalsIgnoreCase(speciesName)) {
            return MCUtil.getPassengerObject(alias, "none");
        }

        return null;
    }

    private static String normalizeForm(String formName) {
        if (formName == null) {
            return "none";
        }

        String normalized = formName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()
                || normalized.equals("normal")
                || normalized.equals("base")
                || normalized.equals("default")) {
            return "none";
        }
        return normalized;
    }

    private static String resolveSpeciesAlias(String speciesName) {
        String compact = speciesName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");

        String special = SPECIAL_SPECIES_ALIASES.get(compact);
        if (special != null) {
            return special;
        }

        for (String prefix : TRANSFORM_PREFIXES) {
            if (compact.startsWith(prefix) && compact.length() > prefix.length()) {
                return compact.substring(prefix.length());
            }
        }

        return speciesName;
    }
}
