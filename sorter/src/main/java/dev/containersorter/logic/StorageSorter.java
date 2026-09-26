package dev.containersorter.logic;

import dev.containersorter.SortMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GenericContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StorageSorter {
    private StorageSorter() {}

    public static boolean isSupported(AbstractContainerMenu menu, Inventory playerInventory) {
        String menuName = menu.getClass().getName().toLowerCase(Locale.ROOT);
        boolean knownStorage = menu instanceof GenericContainerMenu
                || menu instanceof ShulkerBoxMenu
                || menuName.contains("chest");

        if (!knownStorage) {
            return false;
        }

        return storageSlots(menu, playerInventory).size() >= 9;
    }

    public static void sort(ServerPlayer player, AbstractContainerMenu menu, SortMode mode) {
        Inventory playerInventory = player.getInventory();
        if (!isSupported(menu, playerInventory)) {
            return;
        }

        List<Slot> targetSlots = storageSlots(menu, playerInventory);
        List<ItemStack> stacks = new ArrayList<>();

        for (Slot slot : targetSlots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        stacks.sort(comparator(mode));
        List<ItemStack> compacted = compact(stacks);

        for (int i = 0; i < targetSlots.size(); i++) {
            targetSlots.get(i).set(i < compacted.size() ? compacted.get(i) : ItemStack.EMPTY);
        }

        menu.broadcastChanges();
    }

    private static List<Slot> storageSlots(AbstractContainerMenu menu, Inventory playerInventory) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container != playerInventory) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static Comparator<ItemStack> comparator(SortMode mode) {
        Comparator<ItemStack> alpha = Comparator
                .comparing(StorageSorter::registryPath)
                .thenComparing(StorageSorter::namespace);

        Comparator<ItemStack> byMod = Comparator
                .comparing(StorageSorter::namespace)
                .thenComparing(StorageSorter::registryPath);

        Comparator<ItemStack> byType = Comparator
                .comparingInt(StorageSorter::typeGroup)
                .thenComparing(StorageSorter::namespace)
                .thenComparing(StorageSorter::registryPath);

        return switch (mode) {
            case ALPHABETICAL -> alpha;
            case MOD -> byMod;
            case TYPE_MOD_ALPHABETICAL -> byType;
        };
    }

    private static List<ItemStack> compact(List<ItemStack> sorted) {
        List<ItemStack> result = new ArrayList<>();

        for (ItemStack original : sorted) {
            ItemStack remaining = original.copy();

            if (!result.isEmpty()) {
                ItemStack previous = result.get(result.size() - 1);
                if (ItemStack.isSameItemSameTags(previous, remaining)) {
                    int capacity = previous.getMaxStackSize() - previous.getCount();
                    if (capacity > 0) {
                        int moved = Math.min(capacity, remaining.getCount());
                        previous.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }

            while (!remaining.isEmpty()) {
                int amount = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                ItemStack part = remaining.copy();
                part.setCount(amount);
                result.add(part);
                remaining.shrink(amount);
            }
        }

        return result;
    }

    private static String namespace(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getNamespace();
    }

    private static String registryPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    private static int typeGroup(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem) return 0;
        if (stack.isEdible()) return 1;
        if (item instanceof DiggerItem) return 2;
        if (item instanceof SwordItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem) return 3;
        if (item instanceof ArmorItem) return 4;
        if (item instanceof PotionItem) return 5;
        if (item instanceof EnchantedBookItem || item instanceof BookItem) return 6;
        return 7;
    }
}
