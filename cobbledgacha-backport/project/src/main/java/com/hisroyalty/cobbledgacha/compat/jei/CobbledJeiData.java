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

    public static List<MachineRewardRecipe> machineRewards() {
        List<MachineRewardRecipe> out = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            if (i == 4) continue;
            String machineName = i == 1 ? "gacha_machine" : "gacha_machine_" + i;
            Item machine = item("cobbledgacha:" + machineName);
            if (machine == null) continue;
            List<ItemStack> currencies = currencies(i);
            int cost = cost(i);
            JsonObject table = readObject("/data/cobbledgacha/loot_tables/" + machineName + ".json");
            addLootRecipes(out, new ItemStack(machine), currencies, cost, table);
        }

        Item machine12 = item("cobbledgacha:gacha_machine_12");
        if (machine12 != null) {
            for (String yarn : YARNS) {
                Item yarnItem = item("cobbledgacha:" + yarn);
                if (yarnItem == null) continue;
                JsonObject table = readObject("/data/cobbledgacha/loot_tables/gacha_machine_12_" + yarn + ".json");
                addLootRecipes(out, new ItemStack(machine12), List.of(new ItemStack(yarnItem)), cost(12), table);
            }
        }
        return out;
    }

    public static List<CapsuleRewardRecipe> capsuleRewards() {
        List<CapsuleRewardRecipe> out = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String name = "capsule_a" + i;
            Item capsule = item("cobbledgacha:" + name);
            if (capsule == null) continue;
            JsonObject table = readObject("/data/cobbledgacha/loot_tables/gacha_capsules/" + name + ".json");
            if (table == null) continue;
            List<LootEntry> entries = entries(table);
            int total = entries.stream().mapToInt(LootEntry::weight).filter(w -> w > 0).sum();
            if (total <= 0) continue;
            for (LootEntry e : entries) {
                if (e.weight <= 0) continue;
                Item reward = item(e.itemId);
                if (reward == null) continue;
                out.add(new CapsuleRewardRecipe(new ItemStack(capsule), new ItemStack(reward), 100.0F * e.weight / total));
            }
        }
        return out;
    }

    public static List<PokemonGachaRecipe> pokemonRewards() {
        List<PokemonGachaRecipe> out = new ArrayList<>();
        JsonArray arr = readArray("/assets/cobbledgacha/jei/spawn_index.json");
        if (arr == null) return out;

        List<SpawnEntry> entries = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!"gacha_machine_4".equals(str(o, "pool", ""))) continue;
            entries.add(new SpawnEntry(
                str(o, "species", ""),
                str(o, "bucket", "common"),
                dbl(o, "weight", 1.0),
                integer(o, "minLevel", 1),
                integer(o, "maxLevel", 1)
            ));
        }
        if (entries.isEmpty()) return out;

        Map<String, Double> totals = new HashMap<>();
        for (SpawnEntry e : entries) totals.merge(e.bucket, Math.max(0.0, e.weight), Double::sum);
        Map<String, Integer> bucketWeights = bucketWeights();
        int bucketTotal = totals.keySet().stream().mapToInt(b -> Math.max(0, bucketWeights.getOrDefault(b, 1))).sum();
        if (bucketTotal <= 0) bucketTotal = totals.size();

        Item machine = item("cobbledgacha:gacha_machine_4");
        if (machine == null) return out;
        List<ItemStack> currency = currencies(4);

        for (SpawnEntry e : entries) {
            double inBucket = totals.getOrDefault(e.bucket, 0.0);
            if (inBucket <= 0) continue;
            int bw = Math.max(0, bucketWeights.getOrDefault(e.bucket, 1));
            float chance = (float)(100.0 * (bw / (double)bucketTotal) * (Math.max(0.0, e.weight) / inBucket));
            out.add(new PokemonGachaRecipe(new ItemStack(machine), currency, e.species, e.bucket,
                e.minLevel, e.maxLevel, chance, cost(4)));
        }
        return out;
    }

    private static void addLootRecipes(List<MachineRewardRecipe> out, ItemStack machine, List<ItemStack> currencies, int cost, JsonObject table) {
        if (table == null) return;
        List<LootEntry> entries = entries(table);
        int total = entries.stream().mapToInt(LootEntry::weight).filter(w -> w > 0).sum();
        if (total <= 0) return;
        for (LootEntry e : entries) {
            if (e.weight <= 0) continue;
            Item reward = item(e.itemId);
            if (reward == null) continue;
            out.add(new MachineRewardRecipe(machine.copy(), copy(currencies), new ItemStack(reward), 100.0F * e.weight / total, cost));
        }
    }

    private static List<ItemStack> currencies(int machine) {
        String file = machine == 1 ? "currency_items.json" : "currency_items_" + machine + ".json";
        JsonObject tag = readObject("/data/cobbledgacha/tags/items/" + file);
        if (tag == null || !tag.has("values")) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (JsonElement el : tag.getAsJsonArray("values")) {
            if (!el.isJsonPrimitive()) continue;
            Item value = item(el.getAsString());
            if (value != null) result.add(new ItemStack(value));
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
        map.put("common",100); map.put("uncommon",40); map.put("rare",15); map.put("ultra_rare",5); map.put("legendary",1);
        JsonObject cfg = readObject("/data/cobbledgacha/config/server_config.json");
        if (cfg != null && cfg.has("buckets") && cfg.get("buckets").isJsonObject()) {
            for (var e : cfg.getAsJsonObject("buckets").entrySet()) {
                try { map.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static List<LootEntry> entries(JsonObject table) {
        List<LootEntry> out = new ArrayList<>();
        JsonArray pools = table.getAsJsonArray("pools");
        if (pools == null) return out;
        for (JsonElement poolEl : pools) {
            if (!poolEl.isJsonObject()) continue;
            JsonArray entries = poolEl.getAsJsonObject().getAsJsonArray("entries");
            if (entries == null) continue;
            for (JsonElement entryEl : entries) {
                if (!entryEl.isJsonObject()) continue;
                JsonObject e = entryEl.getAsJsonObject();
                if (!e.has("name")) continue;
                out.add(new LootEntry(e.get("name").getAsString(), e.has("weight") ? Math.max(0, e.get("weight").getAsInt()) : 1));
            }
        }
        return out;
    }

    private static JsonObject readObject(String path) {
        try (InputStream in = CobbledJeiData.class.getResourceAsStream(path)) {
            if (in == null) return null;
            JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception e) { return null; }
    }

    private static JsonArray readArray(String path) {
        try (InputStream in = CobbledJeiData.class.getResourceAsStream(path)) {
            if (in == null) return null;
            JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return el.isJsonArray() ? el.getAsJsonArray() : null;
        } catch (Exception e) { return null; }
    }

    private static Item item(String id) {
        try {
            Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return value == null || value == Items.AIR ? null : value;
        } catch (Exception e) { return null; }
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        stacks.forEach(s -> out.add(s.copy()));
        return out;
    }

    private static String str(JsonObject o, String k, String d) { try { return o.has(k) ? o.get(k).getAsString() : d; } catch (Exception e) { return d; } }
    private static int integer(JsonObject o, String k, int d) { try { return o.has(k) ? o.get(k).getAsInt() : d; } catch (Exception e) { return d; } }
    private static double dbl(JsonObject o, String k, double d) { try { return o.has(k) ? o.get(k).getAsDouble() : d; } catch (Exception e) { return d; } }

    private record LootEntry(String itemId, int weight) {}
    private record SpawnEntry(String species, String bucket, double weight, int minLevel, int maxLevel) {}
}
