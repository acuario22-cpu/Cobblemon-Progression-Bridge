package dev.cobblemonbridge.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Variant resolver used by Xaero's Minimap for Cobblemon entities.
 *
 * <p>The important difference from the older resolver is that candidate
 * variant IDs are validated against Xaero's own pokemon.json variant table,
 * not against the presence of a physical Cobblemon texture. This matters for
 * resource packs such as Generations HD, where the visible model/texture can
 * live in another namespace while Xaero still maps a canonical
 * cobblemon:textures/pokemon/... variant ID to a sprite.</p>
 */
public final class XaeroPokemonVariantResolver {
    private static final String PREFIX = "cobblemon:textures/pokemon/";
    private static final ResourceLocation XAERO_DEFINITION =
            new ResourceLocation("xaerominimap", "entity/icon/definition/cobblemon/pokemon.json");

    private static ResourceManager cachedManager;
    private static VariantIndex cachedIndex = VariantIndex.empty();

    private XaeroPokemonVariantResolver() {
    }

    public static void appendVariantId(StringBuilder output, EntityRenderer<?> renderer, Entity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon == null || pokemon.getSpecies() == null || pokemon.getSpecies().getResourceIdentifier() == null) {
            return;
        }

        String species = pokemon.getSpecies().getResourceIdentifier().getPath();
        if (species == null || species.isBlank()) {
            return;
        }
        species = species.toLowerCase(Locale.ROOT);

        boolean shiny = pokemon.getShiny();
        ResourceManager resources = net.minecraft.client.Minecraft.getInstance().getResourceManager();
        VariantIndex index = getIndex(resources);

        for (String candidate : candidates(pokemon, species, shiny)) {
            if (index.contains(candidate)) {
                output.append(candidate);
                return;
            }
        }

        // If a shiny-specific mapping does not exist, prefer a mapped regular
        // icon for the same species instead of allowing Xaero to fall back to
        // a yellow diamond.
        if (shiny) {
            for (String candidate : candidates(pokemon, species, false)) {
                if (index.contains(candidate)) {
                    output.append(candidate);
                    return;
                }
            }
        }

        String mappedFallback = index.fallback(species, shiny);
        if (mappedFallback != null) {
            output.append(mappedFallback);
            return;
        }

        // Absolute compatibility fallback. The fixed resource pack also adds
        // a Xaero "default" entry, so even an unknown add-on species still
        // receives an icon rather than the missing-variant marker.
        output.append(PREFIX).append(species).append("/").append(species);
        if (shiny) {
            output.append("_shiny");
        }
        output.append(".png");
    }

    private static List<String> candidates(Pokemon pokemon, String species, boolean shiny) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String base = PREFIX + species + "/" + species;
        String shinySuffix = shiny ? "_shiny" : "";

        // Forms whose Cobblemon naming is not reliably reproduced by simply
        // concatenating form/aspect tokens.
        if (species.equals("deerling") || species.equals("sawsbuck")) {
            String season = season(pokemon);
            result.add(base + "_" + season + shinySuffix + ".png");
        }

        if (species.equals("frillish") || species.equals("jellicent")) {
            String gender = pokemon.getGender() == Gender.FEMALE ? "_female" : "_male";
            result.add(base + gender + shinySuffix + ".png");
        }

        if (species.equals("gimmighoul")) {
            String form = formAndAspects(pokemon);
            String variant = form.contains("roaming") ? "_roaming" : "_chest";
            result.add(base + variant + shinySuffix + ".png");
        }

        if (species.equals("shellos") || species.equals("gastrodon")) {
            String form = formAndAspects(pokemon);
            String sea = form.contains("west") ? "west" : "east";
            result.add(base + "_" + sea + shinySuffix + ".png");
        }

        if (species.equals("basculin")) {
            String form = formAndAspects(pokemon);
            String stripe = form.contains("white") ? "whitestripe"
                    : (form.contains("blue") ? "bluestripe" : "redstripe");
            result.add(base + "_" + stripe + shinySuffix + ".png");
        }

        for (String suffix : suffixes(pokemon)) {
            result.add(base + suffix + shinySuffix + ".png");
        }

        // Always try the canonical species base last.
        result.add(base + shinySuffix + ".png");
        return new ArrayList<>(result);
    }

    private static String season(Pokemon pokemon) {
        String value = formAndAspects(pokemon);
        if (value.contains("summer")) {
            return "summer";
        }
        if (value.contains("autumn") || value.contains("fall")) {
            return "autumn";
        }
        if (value.contains("winter")) {
            return "winter";
        }
        return "spring";
    }

    private static String formAndAspects(Pokemon pokemon) {
        StringBuilder value = new StringBuilder();
        if (pokemon.getForm() != null) {
            value.append(normalise(pokemon.getForm().getName()));
        }
        for (String aspect : pokemon.getAspects()) {
            value.append(normalise(aspect));
        }
        return value.toString();
    }

    private static List<String> suffixes(Pokemon pokemon) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();

        String form = normalise(pokemon.getForm() == null ? "" : pokemon.getForm().getName());
        if (!form.isEmpty() && !form.equals("standard") && !form.equals("normal")) {
            tokens.add(form);
            addRegionAliases(tokens, form);
            addFormAliases(tokens, form);
        }

        for (String aspect : pokemon.getAspects()) {
            String value = normalise(aspect);
            if (value.isEmpty() || value.equals("shiny") || value.equals("standard") || value.equals("normal")) {
                continue;
            }
            tokens.add(value);
            addRegionAliases(tokens, value);
            addFormAliases(tokens, value);
        }

        if (pokemon.getSpecies().getResourceIdentifier().getPath().equals("flabebe")) {
            for (String token : new ArrayList<>(tokens)) {
                for (String colour : List.of("red", "yellow", "orange", "blue", "white")) {
                    if (token.contains(colour)) {
                        tokens.add(colour);
                    }
                }
            }
        }

        ArrayList<String> values = new ArrayList<>(tokens);
        values.sort(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));

        LinkedHashSet<String> suffixes = new LinkedHashSet<>();
        addSuffixes(suffixes, values, "", 0);

        if (pokemon.getGender() == Gender.FEMALE) {
            ArrayList<String> beforeGender = new ArrayList<>(suffixes);
            for (String suffix : beforeGender) {
                suffixes.add(suffix + "_female");
                suffixes.add("_female" + suffix);
            }
            suffixes.add("_female");
        }

        // A few packs explicitly include male variants too. Generating these
        // is harmless because candidates are accepted only if pokemon.json
        // actually contains them.
        if (pokemon.getGender() == Gender.MALE) {
            ArrayList<String> beforeGender = new ArrayList<>(suffixes);
            for (String suffix : beforeGender) {
                suffixes.add(suffix + "_male");
                suffixes.add("_male" + suffix);
            }
            suffixes.add("_male");
        }

        suffixes.add("");
        return new ArrayList<>(suffixes);
    }

    private static void addRegionAliases(Set<String> tokens, String value) {
        if (value.contains("alola")) {
            tokens.add("alolan");
            tokens.add("alola");
        }
        if (value.contains("galar")) {
            tokens.add("galarian");
            tokens.add("galar");
        }
        if (value.contains("hisui")) {
            tokens.add("hisuian");
            tokens.add("hisui");
        }
        if (value.contains("paldea")) {
            tokens.add("paldean");
            tokens.add("paldea");
        }
    }

    private static void addFormAliases(Set<String> tokens, String value) {
        if (value.contains("gigantamax") || value.equals("gmax")) {
            tokens.add("gigantamax");
            tokens.add("gmax");
        }
        if (value.contains("megax") || value.contains("megaxform")) {
            tokens.add("megax");
        }
        if (value.contains("megay") || value.contains("megayform")) {
            tokens.add("megay");
        }
    }

    private static void addSuffixes(LinkedHashSet<String> output, List<String> tokens, String current, int depth) {
        if (!current.isEmpty()) {
            output.add("_" + current);
        }
        if (depth >= 4 || tokens.isEmpty()) {
            return;
        }

        for (int index = 0; index < tokens.size(); index++) {
            ArrayList<String> rest = new ArrayList<>(tokens);
            String next = rest.remove(index);
            String combined = current.isEmpty() ? next : current + "_" + next;
            addSuffixes(output, rest, combined, depth + 1);

            if (!current.isEmpty()) {
                addSuffixes(output, rest, current + next, depth + 1);
            }
        }
    }

    private static String normalise(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static VariantIndex getIndex(ResourceManager resources) {
        synchronized (XaeroPokemonVariantResolver.class) {
            if (resources == cachedManager) {
                return cachedIndex;
            }

            cachedManager = resources;
            cachedIndex = readIndex(resources);
            return cachedIndex;
        }
    }

    private static VariantIndex readIndex(ResourceManager resources) {
        try {
            Optional<Resource> resource = resources.getResource(XAERO_DEFINITION);
            if (resource.isEmpty()) {
                return VariantIndex.empty();
            }

            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    return VariantIndex.empty();
                }

                JsonObject root = rootElement.getAsJsonObject();
                JsonObject variants = root.getAsJsonObject("variants");
                if (variants == null) {
                    return VariantIndex.empty();
                }

                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (String key : variants.keySet()) {
                    if (key.startsWith(PREFIX)) {
                        ids.add(key);
                    }
                }
                return new VariantIndex(ids);
            }
        } catch (Exception ignored) {
            return VariantIndex.empty();
        }
    }

    private static final class VariantIndex {
        private final Set<String> ids;

        private VariantIndex(Set<String> ids) {
            this.ids = ids;
        }

        static VariantIndex empty() {
            return new VariantIndex(Set.of());
        }

        boolean contains(String id) {
            return ids.contains(id);
        }

        String fallback(String species, boolean shiny) {
            String prefix = PREFIX + species + "/";
            String base = prefix + species;
            String exact = base + (shiny ? "_shiny" : "") + ".png";
            if (ids.contains(exact)) {
                return exact;
            }

            String spring = base + "_spring" + (shiny ? "_shiny" : "") + ".png";
            if (ids.contains(spring)) {
                return spring;
            }

            String best = null;
            int bestScore = Integer.MAX_VALUE;
            for (String id : ids) {
                if (!id.startsWith(prefix)) {
                    continue;
                }

                boolean idShiny = id.contains("_shiny");
                int score;
                if (shiny && idShiny) {
                    score = 10;
                } else if (!shiny && !idShiny) {
                    score = 10;
                } else if (shiny) {
                    score = 40;
                } else {
                    score = 80;
                }

                // Prefer canonical species filenames over unusual alternate
                // folder entries and then keep the choice deterministic.
                if (id.startsWith(base)) {
                    score -= 3;
                }
                score += Math.min(20, id.length() - prefix.length());

                if (score < bestScore || (score == bestScore && (best == null || id.compareTo(best) < 0))) {
                    best = id;
                    bestScore = score;
                }
            }
            return best;
        }
    }
}
