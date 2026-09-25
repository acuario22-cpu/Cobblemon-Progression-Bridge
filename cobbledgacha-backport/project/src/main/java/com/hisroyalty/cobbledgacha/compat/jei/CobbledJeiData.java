package com.hisroyalty.cobbledgacha.compat.jei;

import com.google.gson.*;
import com.hisroyalty.cobbledgacha.CobbledGacha;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CobbledJeiData {
    private CobbledJeiData() {}

    private static final String[] YARNS = {
        "creepy_yarn","fantasy_yarn","feathery_yarn","fiery_yarn","frosty_yarn","grassy_yarn",
        "hardy_yarn","plain_yarn","soggy_yarn","sparky_yarn","toothy_yarn"
    };

    private static final int POKEMON_PER_PAGE = 7;

    public static List<MachineRewardRecipe> machineRewards() {
        List<MachineRewardRecipe> out = new ArrayList<>();

        // Poké Gacha colour variants 1 and 5-10 are cosmetic equivalents.
        addLootRecipes(
            out,
            machines(1,5,6,7,8,9,10),
            currencies(1),
            cost(1),
            readObject("/data/cobbledgacha/loot_tables/gacha_machine.json")
        );

        addLootRecipes(
            out,
            machines(2),
            currencies(2),
            cost(2),
            readObject("/data/cobbledgacha/loot_tables/gacha_machine_2.json")
        );

        addLootRecipes(
            out,
            machines(3),
            currencies(3),
            cost(3),
            readObject("/data/cobbledgacha/loot_tables/gacha_machine_3.json")
        );

        addLootRecipes(
            out,
            machines(11),
            currencies(11),
            cost(11),
            readObject("/data/cobbledgacha/loot_tables/gacha_machine_11.json")
        );

        return out;
    }

    public static List<PlushRecipe> plushRewards() {
        List<PlushRecipe> out = new ArrayList<>();
        Item machine = item("cobbledgacha:gacha_machine_12");
        if (machine == null) return out;

        for (String yarnName : YARNS) {
            Item yarn = item("cobbledgacha:" + yarnName);
            if (yarn == null) continue;

            JsonObject table = readObject(
                "/data/cobbledgacha/loot_tables/gacha_machine_12_" + yarnName + ".json"
            );
            if (table == null) continue;

            List<LootEntry> entries = entries(table);
            int total = entries.stream()
                .mapToInt(LootEntry::weight)
                .filter(weight -> weight > 0)
                .sum();
            if (total <= 0) continue;

            for (LootEntry entry : entries) {
                if (entry.weight <= 0) continue;
                Item doll = item(entry.itemId);
                if (doll == null) continue;

                ItemStack yarnStack = new ItemStack(yarn, 3);
                out.add(new PlushRecipe(
                    new ItemStack(machine),
                    yarnStack,
                    new ItemStack(doll),
                    dollVariant(entry.itemId),
                    100.0F * entry.weight / total
                ));
            }
        }

        return out;
    }

    public static List<CapsuleRewardRecipe> capsuleRewards() {
        List<CapsuleRewardRecipe> out = new ArrayList<>();

        for (int i = 1; i <= CobbledGacha.USEFUL_CAPSULE_COUNT; i++) {
            String name = "capsule_a" + i;
            Item capsule = item("cobbledgacha:" + name);
            if (capsule == null) continue;

            JsonObject table = readObject(
                "/data/cobbledgacha/loot_tables/gacha_capsules/" + name + ".json"
            );
            if (table == null) continue;

            List<LootEntry> entries = entries(table);
            int total = entries.stream()
                .mapToInt(LootEntry::weight)
                .filter(weight -> weight > 0)
                .sum();
            if (total <= 0) continue;

            for (LootEntry entry : entries) {
                if (entry.weight <= 0) continue;
                Item reward = item(entry.itemId);
                if (reward == null) continue;

                out.add(new CapsuleRewardRecipe(
                    new ItemStack(capsule),
                    new ItemStack(reward),
                    100.0F * entry.weight / total
                ));
            }
        }

        return out;
    }

    public static List<PokemonGachaRecipe> pokemonRewards() {
        List<PokemonGachaRecipe> out = new ArrayList<>();
        List<PokemonRewardLine> lines = pokemonLinesForPool("gacha_machine_4");
        if (lines.isEmpty()) return out;

        Item machine = item("cobbledgacha:gacha_machine_4");
        if (machine == null) return out;

        List<ItemStack> currency = currencies(4);
        for (int start = 0; start < lines.size(); start += POKEMON_PER_PAGE) {
            int end = Math.min(lines.size(), start + POKEMON_PER_PAGE);
            out.add(new PokemonGachaRecipe(
                new ItemStack(machine),
                copy(currency),
                List.copyOf(lines.subList(start, end)),
                cost(4)
            ));
        }

        return out;
    }

    public static List<BallPokemonRecipe> ballPokemonRewards() {
        List<BallPokemonRecipe> out = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            String itemName = "gacha_ball_" + i;
            Item ball = item("cobbledgacha:" + itemName);
            if (ball == null) continue;

            List<PokemonRewardLine> lines = pokemonLinesForPool(itemName);
            addBallPages(out, new ItemStack(ball), lines);
        }

        Item rocketBall = item("cobbledgacha:rocket_ball");
        if (rocketBall != null) {
            addBallPages(
                out,
                new ItemStack(rocketBall),
                pokemonLinesForPool("rocket_ball")
            );
        }

        return out;
    }

    public static List<ItemStack> hiddenCapsules() {
        List<ItemStack> hidden = new ArrayList<>();
        for (int i = CobbledGacha.USEFUL_CAPSULE_COUNT; i < CobbledGacha.CAPSULES.size(); i++) {
            hidden.add(new ItemStack(CobbledGacha.CAPSULES.get(i).get()));
        }
        return hidden;
    }

    private static void addBallPages(
        List<BallPokemonRecipe> out,
        ItemStack ball,
        List<PokemonRewardLine> lines
    ) {
        if (lines.isEmpty()) return;

        for (int start = 0; start < lines.size(); start += POKEMON_PER_PAGE) {
            int end = Math.min(lines.size(), start + POKEMON_PER_PAGE);
            out.add(new BallPokemonRecipe(
                ball.copy(),
                List.copyOf(lines.subList(start, end))
            ));
        }
    }

    private static List<PokemonRewardLine> pokemonLinesForPool(String poolName) {
        JsonArray index = readArray("/assets/cobbledgacha/jei/spawn_index.json");
        if (index == null) return List.of();

        List<SpawnEntry> entries = new ArrayList<>();
        for (JsonElement element : index) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!poolName.equals(str(object, "pool", ""))) continue;

            String species = str(object, "species", "");
            if (species.isBlank()) continue;

            entries.add(new SpawnEntry(
                species,
                str(object, "bucket", "common"),
                Math.max(0.0, dbl(object, "weight", 1.0)),
                integer(object, "minLevel", 1),
                integer(object, "maxLevel", 1)
            ));
        }

        if (entries.isEmpty()) return List.of();

        Map<String, Double> totals = new HashMap<>();
        for (SpawnEntry entry : entries) {
            totals.merge(entry.bucket, entry.weight, Double::sum);
        }

        Map<String, Integer> bucketWeights = bucketWeights();
        int bucketTotal = totals.keySet().stream()
            .mapToInt(bucket -> Math.max(0, bucketWeights.getOrDefault(bucket, 1)))
            .sum();
        if (bucketTotal <= 0) {
            bucketTotal = Math.max(1, totals.size());
        }

        List<PokemonRewardLine> lines = new ArrayList<>();
        for (SpawnEntry entry : entries) {
            double inBucket = totals.getOrDefault(entry.bucket, 0.0);
            if (inBucket <= 0 || entry.weight <= 0) continue;

            int bucketWeight = Math.max(0, bucketWeights.getOrDefault(entry.bucket, 1));
            float chance = (float) (
                100.0
                * (bucketWeight / (double) bucketTotal)
                * (entry.weight / inBucket)
            );

            lines.add(new PokemonRewardLine(
                entry.species,
                entry.bucket,
                entry.minLevel,
                Math.max(entry.minLevel, entry.maxLevel),
                chance
            ));
        }

        Map<String, Integer> order = Map.of(
            "common", 0,
            "uncommon", 1,
            "rare", 2,
            "ultra_rare", 3,
            "legendary", 4
        );

        lines.sort(
            Comparator
                .comparingInt((PokemonRewardLine entry) ->
                    order.getOrDefault(entry.bucket(), 99))
                .thenComparing(PokemonRewardLine::species)
        );

        return lines;
    }

    private static String dollVariant(String itemId) {
        String path = itemId.contains(":")
            ? itemId.substring(itemId.indexOf(':') + 1)
            : itemId;

        if (path.startsWith("gigantic_pokedoll_shiny_")) {
            return "jei.cobbledgacha.variant.gigantic_shiny";
        }
        if (path.startsWith("gigantic_pokedoll_")) {
            return "jei.cobbledgacha.variant.gigantic";
        }
        if (path.startsWith("pokedoll_shiny_")) {
            return "jei.cobbledgacha.variant.shiny";
        }
        return "jei.cobbledgacha.variant.normal";
    }

    private static List<ItemStack> machines(int... ids) {
        List<ItemStack> result = new ArrayList<>();
        for (int id : ids) {
            String name = id == 1 ? "gacha_machine" : "gacha_machine_" + id;
            var machine = CobbledGacha.MACHINES.get(name);
            if (machine != null) {
                result.add(new ItemStack(machine.get()));
            }
        }
        return result;
    }

    private static void addLootRecipes(
        List<MachineRewardRecipe> out,
        List<ItemStack> machines,
        List<ItemStack> currencies,
        int cost,
        JsonObject table
    ) {
        if (table == null || machines.isEmpty()) return;

        List<LootEntry> entries = entries(table);
        int total = entries.stream()
            .mapToInt(LootEntry::weight)
            .filter(weight -> weight > 0)
            .sum();
        if (total <= 0) return;

        for (LootEntry entry : entries) {
            if (entry.weight <= 0) continue;
            Item reward = item(entry.itemId);
            if (reward == null) continue;

            out.add(new MachineRewardRecipe(
                copy(machines),
                copy(currencies),
                new ItemStack(reward),
                100.0F * entry.weight / total,
                cost
            ));
        }
    }

    private static List<ItemStack> currencies(int machine) {
        String file = machine == 1
            ? "currency_items.json"
            : "currency_items_" + machine + ".json";

        JsonObject tag = readObject(
            "/data/cobbledgacha/tags/items/" + file
        );
        if (tag == null || !tag.has("values")) return List.of();

        List<ItemStack> result = new ArrayList<>();
        for (JsonElement element : tag.getAsJsonArray("values")) {
            if (!element.isJsonPrimitive()) continue;
            Item value = item(element.getAsString());
            if (value != null) {
                result.add(new ItemStack(value));
            }
        }
        return result;
    }

    private static int cost(int machine) {
        return switch (machine) {
            case 2, 3 -> 20;
            case 11 -> 1;
            case 12 -> 3;
            default -> 5;
        };
    }

    private static Map<String, Integer> bucketWeights() {
        Map<String, Integer> map = new HashMap<>();
        map.put("common", 100);
        map.put("uncommon", 40);
        map.put("rare", 15);
        map.put("ultra_rare", 5);
        map.put("legendary", 1);

        JsonObject config = readObject(
            "/data/cobbledgacha/config/server_config.json"
        );
        if (config != null
            && config.has("buckets")
            && config.get("buckets").isJsonObject()) {
            for (var entry : config.getAsJsonObject("buckets").entrySet()) {
                try {
                    map.put(entry.getKey(), entry.getValue().getAsInt());
                } catch (Exception ignored) {}
            }
        }

        return map;
    }

    private static List<LootEntry> entries(JsonObject table) {
        List<LootEntry> out = new ArrayList<>();
        JsonArray pools = table.getAsJsonArray("pools");
        if (pools == null) return out;

        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) continue;
            JsonArray entryArray = poolElement
                .getAsJsonObject()
                .getAsJsonArray("entries");
            if (entryArray == null) continue;

            for (JsonElement entryElement : entryArray) {
                if (!entryElement.isJsonObject()) continue;
                JsonObject entry = entryElement.getAsJsonObject();
                if (!entry.has("name")) continue;

                out.add(new LootEntry(
                    entry.get("name").getAsString(),
                    entry.has("weight")
                        ? Math.max(0, entry.get("weight").getAsInt())
                        : 1
                ));
            }
        }

        return out;
    }

    private static JsonObject readObject(String path) {
        try (InputStream input = CobbledJeiData.class.getResourceAsStream(path)) {
            if (input == null) return null;
            JsonElement element = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            );
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonArray readArray(String path) {
        try (InputStream input = CobbledJeiData.class.getResourceAsStream(path)) {
            if (input == null) return null;
            JsonElement element = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            );
            return element.isJsonArray() ? element.getAsJsonArray() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Item item(String id) {
        try {
            Item value = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(id)
            );
            return value == null || value == Items.AIR ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        stacks.forEach(stack -> out.add(stack.copy()));
        return out;
    }

    private static String str(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double dbl(JsonObject object, String key, double fallback) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private record LootEntry(String itemId, int weight) {}
    private record SpawnEntry(
        String species,
        String bucket,
        double weight,
        int minLevel,
        int maxLevel
    ) {}
}
