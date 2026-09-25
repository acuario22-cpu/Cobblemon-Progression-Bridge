package com.hisroyalty.gachamachine.compat.jei;

import com.google.gson.*;
import com.hisroyalty.gachamachine.GachaMachine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GachaJeiData {
    private GachaJeiData() {}

    public static List<GachaRewardRecipe> machineRewards() {
        List<GachaRewardRecipe> recipes = new ArrayList<>();
        List<ItemStack> machines = GachaMachine.MACHINES.values().stream().map(v -> new ItemStack(v.get())).toList();
        List<ItemStack> currencies = GachaMachine.COINS.values().stream().map(v -> new ItemStack(v.get())).toList();

        JsonObject table = read("/data/gachamachine/loot_tables/gacha_machine.json");
        if (table == null) return recipes;
        List<Entry> entries = entries(table);
        int total = entries.stream().mapToInt(Entry::weight).filter(w -> w > 0).sum();
        if (total <= 0) return recipes;

        for (Entry entry : entries) {
            if (entry.weight <= 0) continue;
            Item reward = item(entry.itemId);
            if (reward == null) continue;
            recipes.add(new GachaRewardRecipe(
                copy(machines),
                copy(currencies),
                new ItemStack(reward),
                100.0F * entry.weight / total));
        }
        return recipes;
    }

    public static List<CapsuleRewardRecipe> capsuleRewards() {
        List<CapsuleRewardRecipe> recipes = new ArrayList<>();
        for (String capsuleName : GachaMachine.USEFUL_CAPSULES) {
            Item capsule = item("gachamachine:" + capsuleName);
            if (capsule == null) continue;

            JsonObject table = read("/data/gachamachine/loot_tables/gacha_capsules/" + capsuleName + ".json");
            if (table == null) continue;
            List<Entry> entries = entries(table);
            int total = entries.stream().mapToInt(Entry::weight).filter(w -> w > 0).sum();
            if (total <= 0) continue;

            for (Entry entry : entries) {
                if (entry.weight <= 0) continue;
                Item reward = item(entry.itemId);
                if (reward == null) continue;
                recipes.add(new CapsuleRewardRecipe(
                    new ItemStack(capsule),
                    new ItemStack(reward),
                    100.0F * entry.weight / total));
            }
        }
        return recipes;
    }

    public static List<ItemStack> hiddenCapsules() {
        List<ItemStack> hidden = new ArrayList<>();
        for (var entry : GachaMachine.CAPSULES.entrySet()) {
            if (!GachaMachine.USEFUL_CAPSULES.contains(entry.getKey())) {
                hidden.add(new ItemStack(entry.getValue().get()));
            }
        }
        return hidden;
    }

    private static JsonObject read(String path) {
        try (InputStream in = GachaJeiData.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Entry> entries(JsonObject table) {
        List<Entry> out = new ArrayList<>();
        JsonArray pools = table.getAsJsonArray("pools");
        if (pools == null) return out;
        for (JsonElement poolEl : pools) {
            if (!poolEl.isJsonObject()) continue;
            JsonArray list = poolEl.getAsJsonObject().getAsJsonArray("entries");
            if (list == null) continue;
            for (JsonElement entryEl : list) {
                if (!entryEl.isJsonObject()) continue;
                JsonObject entry = entryEl.getAsJsonObject();
                if (!entry.has("name")) continue;
                String id = entry.get("name").getAsString();
                int weight = entry.has("weight") ? Math.max(0, entry.get("weight").getAsInt()) : 1;
                out.add(new Entry(id, weight));
            }
        }
        return out;
    }

    private static Item item(String id) {
        try {
            Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return value == null || value == Items.AIR ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        stacks.forEach(s -> out.add(s.copy()));
        return out;
    }

    private record Entry(String itemId, int weight) {}
}
