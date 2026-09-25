package com.hisroyalty.cobbledgacha.loot;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RewardTableManager extends SimplePreparableReloadListener<Map<String, RewardTableManager.Table>> {
    public record Entry(ResourceLocation itemId, int weight, int minCount, int maxCount) {}
    public record Pool(int minRolls, int maxRolls, List<Entry> entries) {}
    public record Table(List<Pool> pools) {}

    private static volatile Map<String, Table> tables = Map.of();

    @Override
    protected Map<String, Table> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Table> result = new HashMap<>();

        Map<ResourceLocation, Resource> resources = manager.listResources(
            "loot_tables",
            id -> id.getNamespace().equals("cobbledgacha")
                && id.getPath().endsWith(".json")
                && !id.getPath().startsWith("loot_tables/blocks/")
        );

        for (var file : resources.entrySet()) {
            String path = file.getKey().getPath();
            if (!path.startsWith("loot_tables/") || !path.endsWith(".json")) continue;

            String key = path.substring("loot_tables/".length(), path.length() - ".json".length());

            try (InputStreamReader reader = new InputStreamReader(file.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                List<Pool> parsedPools = new ArrayList<>();

                JsonArray pools = root.getAsJsonArray("pools");
                if (pools == null) continue;

                for (JsonElement poolElement : pools) {
                    if (!poolElement.isJsonObject()) continue;
                    JsonObject poolObject = poolElement.getAsJsonObject();

                    int[] rolls = parseRolls(poolObject.get("rolls"));
                    List<Entry> parsedEntries = new ArrayList<>();

                    JsonArray entries = poolObject.getAsJsonArray("entries");
                    if (entries == null) continue;

                    for (JsonElement entryElement : entries) {
                        if (!entryElement.isJsonObject()) continue;
                        JsonObject entryObject = entryElement.getAsJsonObject();
                        if (!entryObject.has("name")) continue;

                        ResourceLocation itemId = ResourceLocation.tryParse(entryObject.get("name").getAsString());
                        if (itemId == null) continue;

                        int weight = Math.max(0, getInt(entryObject, "weight", 1));
                        int minCount = 1;
                        int maxCount = 1;

                        JsonArray functions = entryObject.getAsJsonArray("functions");
                        if (functions != null) {
                            for (JsonElement functionElement : functions) {
                                if (!functionElement.isJsonObject()) continue;
                                JsonObject function = functionElement.getAsJsonObject();
                                String functionId = getString(function, "function", "");
                                if (!(functionId.equals("set_count") || functionId.equals("minecraft:set_count"))) continue;

                                int[] count = parseRange(function.get("count"), 1);
                                minCount = Math.max(1, count[0]);
                                maxCount = Math.max(minCount, count[1]);
                            }
                        }

                        parsedEntries.add(new Entry(itemId, weight, minCount, maxCount));
                    }

                    if (!parsedEntries.isEmpty()) {
                        parsedPools.add(new Pool(
                            Math.max(0, rolls[0]),
                            Math.max(rolls[0], rolls[1]),
                            List.copyOf(parsedEntries)
                        ));
                    }
                }

                if (!parsedPools.isEmpty()) {
                    result.put(key, new Table(List.copyOf(parsedPools)));
                }
            } catch (Exception ignored) {
            }
        }

        return Map.copyOf(result);
    }

    @Override
    protected void apply(Map<String, Table> data, ResourceManager manager, ProfilerFiller profiler) {
        tables = data;
    }

    public static boolean hasValidRewards(String tableKey) {
        Table table = tables.get(tableKey);
        if (table == null) return false;

        for (Pool pool : table.pools()) {
            for (Entry entry : pool.entries()) {
                if (resolve(entry.itemId()) != null && entry.weight() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ItemStack> roll(String tableKey, ServerLevel level) {
        Table table = tables.get(tableKey);
        if (table == null) return List.of();

        List<ItemStack> result = new ArrayList<>();

        for (Pool pool : table.pools()) {
            List<Entry> validEntries = pool.entries().stream()
                .filter(entry -> entry.weight() > 0 && resolve(entry.itemId()) != null)
                .toList();

            if (validEntries.isEmpty()) continue;

            int rolls = randomBetween(level.random, pool.minRolls(), pool.maxRolls());
            for (int i = 0; i < rolls; i++) {
                Entry selected = weighted(validEntries, level.random);
                if (selected == null) continue;

                Item item = resolve(selected.itemId());
                if (item == null) continue;

                int count = randomBetween(level.random, selected.minCount(), selected.maxCount());
                addMerged(result, new ItemStack(item, count));
            }
        }

        return result;
    }

    public static Set<ResourceLocation> missingItems(String tableKey) {
        Table table = tables.get(tableKey);
        if (table == null) return Set.of();

        Set<ResourceLocation> missing = new LinkedHashSet<>();
        for (Pool pool : table.pools()) {
            for (Entry entry : pool.entries()) {
                if (resolve(entry.itemId()) == null) {
                    missing.add(entry.itemId());
                }
            }
        }
        return Set.copyOf(missing);
    }

    private static void addMerged(List<ItemStack> stacks, ItemStack addition) {
        int remaining = addition.getCount();

        for (ItemStack existing : stacks) {
            if (remaining <= 0) break;
            if (!ItemStack.isSameItemSameTags(existing, addition)) continue;

            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;

            int move = Math.min(room, remaining);
            existing.grow(move);
            remaining -= move;
        }

        while (remaining > 0) {
            int count = Math.min(addition.getMaxStackSize(), remaining);
            ItemStack copy = addition.copy();
            copy.setCount(count);
            stacks.add(copy);
            remaining -= count;
        }
    }

    private static Entry weighted(List<Entry> entries, net.minecraft.util.RandomSource random) {
        int total = entries.stream().mapToInt(Entry::weight).sum();
        if (total <= 0) return null;

        int value = random.nextInt(total);
        for (Entry entry : entries) {
            value -= entry.weight();
            if (value < 0) return entry;
        }
        return entries.get(entries.size() - 1);
    }

    private static Item resolve(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? null : item;
    }

    private static int randomBetween(net.minecraft.util.RandomSource random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private static int[] parseRolls(JsonElement element) {
        return parseRange(element, 1);
    }

    private static int[] parseRange(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return new int[]{fallback, fallback};
        }

        if (element.isJsonPrimitive()) {
            try {
                int value = Math.max(0, element.getAsInt());
                return new int[]{value, value};
            } catch (Exception ignored) {
                return new int[]{fallback, fallback};
            }
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            int min = getInt(object, "min", fallback);
            int max = getInt(object, "max", min);
            return new int[]{Math.max(0, min), Math.max(min, max)};
        }

        return new int[]{fallback, fallback};
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
