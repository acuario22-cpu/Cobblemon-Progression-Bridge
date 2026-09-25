package com.hisroyalty.gachamachine.compat.jei;

import com.google.gson.*;
import com.hisroyalty.gachamachine.GachaMachine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GachaJeiData {
    private GachaJeiData() {}

    public static List<GachaRewardRecipe> machineRewards() {
        List<GachaRewardRecipe> recipes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String machineName = i == 1 ? "gacha_machine" : "gacha_machine_" + i;
            Item machineItem = item("gachamachine:" + machineName);
            Item currencyItem = item("gachamachine:" + GachaMachine.expectedCoinForMachine(i));
            if (machineItem == null || currencyItem == null) continue;

            JsonObject table = read("/data/gachamachine/loot_tables/" + machineName + ".json");
            if (table == null) continue;
            List<Entry> entries = entries(table);
            int total = entries.stream().mapToInt(Entry::weight).filter(w -> w > 0).sum();
            if (total <= 0) continue;

            for (Entry entry : entries) {
                if (entry.weight <= 0) continue;
                Item reward = item(entry.itemId);
                if (reward == null) continue;
                recipes.add(new GachaRewardRecipe(
                    new ItemStack(machineItem),
                    new ItemStack(currencyItem),
                    new ItemStack(reward),
                    100.0F * entry.weight / total));
            }
        }
        return recipes;
    }

    public static List<CapsuleRewardRecipe> capsuleRewards() {
        List<CapsuleRewardRecipe> recipes = new ArrayList<>();
        for (char row = 'a'; row <= 'j'; row++) {
            for (int col = 1; col <= 10; col++) {
                String capsuleName = "capsule_" + row + col;
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
        }
        return recipes;
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
            return value == null || value == net.minecraft.world.item.Items.AIR ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    private record Entry(String itemId, int weight) {}
}
