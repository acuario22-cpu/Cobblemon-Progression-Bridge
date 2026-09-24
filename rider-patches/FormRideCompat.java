package dev.zanckor.cobblemonrider;

import dev.zanckor.cobblemonrider.config.PokemonJsonObject;

import java.util.Locale;
import java.util.Set;

/**
 * Compatibility-first form resolver. It intentionally follows the same lookup
 * order as the user's working GMAX/Mega Fix V2 and only extends the known
 * fallback forms with Primal support.
 */
public final class FormRideCompat {
    private static final Set<String> MEGAMONS = Set.of(
            "aerodactyl", "alakazam", "beedrill", "blastoise", "charizardx",
            "charizardy", "gengar", "gyarados", "kangaskhan", "mewtwox",
            "mewtwoy", "pidgeot", "pinsir", "slowbro", "venusaur",
            "ampharos", "scizor", "steelix", "tyranitar", "banette",
            "gardevoir", "glalie", "mawile", "sableye", "sharpedo",
            "gallade", "garchomp"
    );

    private FormRideCompat() {
    }

    public static PokemonJsonObject.PokemonConfigData resolve(String speciesName, String formName) {
        if (speciesName == null || formName == null) {
            return null;
        }

        String normalizedForm = formName.toLowerCase(Locale.ROOT);
        if (normalizedForm.equals("normal") || normalizedForm.equals("base")) {
            normalizedForm = "none";
        }

        PokemonJsonObject.PokemonConfigData direct =
                MCUtil.getPassengerObject(speciesName, normalizedForm);
        if (direct != null) {
            return direct;
        }

        String normalizedSpecies = speciesName.toLowerCase(Locale.ROOT);

        // Keep the exact Megamons species-prefix behaviour from V2.
        if (normalizedSpecies.startsWith("mega")) {
            String megaName = normalizedSpecies.substring(4);
            if (MEGAMONS.contains(megaName)) {
                String baseName = megaName;
                if (baseName.equals("charizardx") || baseName.equals("charizardy")) {
                    baseName = "Charizard";
                } else if (baseName.equals("mewtwox") || baseName.equals("mewtwoy")) {
                    baseName = "Mewtwo";
                }
                return MCUtil.getPassengerObject(baseName, "none");
            }
        }

        // Same V2 fallback list, extended with Primal.
        if (normalizedForm.equals("gmax")
                || normalizedForm.equals("gigantamax")
                || normalizedForm.equals("dynamax")
                || normalizedForm.equals("mega")
                || normalizedForm.equals("mega-x")
                || normalizedForm.equals("mega-y")
                || normalizedForm.equals("primal")
                || normalizedForm.equals("primal-kyogre")
                || normalizedForm.equals("primal-groudon")) {
            return MCUtil.getPassengerObject(speciesName, "none");
        }

        // Some addons encode Primal as the species name instead of the form.
        if (normalizedSpecies.startsWith("primal") && normalizedSpecies.length() > 6) {
            String baseName = normalizedSpecies.substring(6);
            return MCUtil.getPassengerObject(baseName, "none");
        }

        return null;
    }
}
