package dev.cobblemodern.tm;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.cobblemodern.network.ModernNetwork;
import dev.cobblemodern.network.MoveEntryData;
import dev.cobblemodern.network.OpenMoveScreenPacket;
import dev.cobblemodern.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class TMSystem {
    public static final String KEY_UNLOCKS = "cmodern_move_unlocks";
    public static final String KEY_DEX_PAGE = "cmodern_movedex_page";
    public static final String KEY_TM_PAGE = "cmodern_tm_page";

    public static Set<String> refreshUnlocks(ServerPlayer player) {
        Set<String> unlocked = loadUnlocks(player);

        try {
            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                registerPokemonMoves(unlocked, pokemon);
            }
        } catch (Throwable ignored) {}

        try {
            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getPC(player.getUUID())) {
                registerPokemonMoves(unlocked, pokemon);
            }
        } catch (Throwable ignored) {}

        saveUnlocks(player, unlocked);
        return unlocked;
    }

    private static void registerPokemonMoves(Set<String> unlocked, Pokemon pokemon) {
        if (pokemon == null) return;
        for (Move move : pokemon.getMoveSet()) {
            if (move != null) unlocked.add(move.getName().toLowerCase(Locale.ROOT));
        }
        for (BenchedMove move : pokemon.getBenchedMoves()) {
            if (move != null && move.getMoveTemplate() != null) {
                unlocked.add(move.getMoveTemplate().getName().toLowerCase(Locale.ROOT));
            }
        }
    }

    public static void openMoveDex(ServerPlayer player) {
        ModernNetwork.open(player, OpenMoveScreenPacket.ScreenMode.MOVE_DEX, buildEntries(player, false));
    }

    public static void openMachine(ServerPlayer player) {
        ModernNetwork.open(player, OpenMoveScreenPacket.ScreenMode.TM_MACHINE, buildEntries(player, true));
    }

    public static void showMoveDex(ServerPlayer player, int ignoredPage) {
        openMoveDex(player);
    }

    public static void showMachine(ServerPlayer player, int ignoredPage) {
        openMachine(player);
    }

    public static List<MoveEntryData> buildEntries(ServerPlayer player, boolean onlyCraftable) {
        Set<String> unlocked = refreshUnlocks(player);
        List<MoveEntryData> out = new ArrayList<>();
        for (String moveId : unlocked) {
            MoveTemplate move = Moves.INSTANCE.getByName(moveId);
            if (move == null) continue;
            boolean craftable = hasTMItem(moveId);
            if (onlyCraftable && !craftable) continue;
            String type = move.getElementalType() == null ? "normal" : move.getElementalType().getName();
            int power = move.getPower() <= 0 ? 0 : (int) move.getPower();
            int accuracy = move.getAccuracy() <= 0 ? 0 : (int) move.getAccuracy();
            out.add(new MoveEntryData(moveId, move.getDisplayName().getString(), type, power, accuracy, craftable));
        }
        out.sort(Comparator.comparing(MoveEntryData::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static boolean craft(ServerPlayer player, String rawMove) {
        String name = rawMove.toLowerCase(Locale.ROOT).replace(" ", "");
        Set<String> unlocked = refreshUnlocks(player);
        if (!unlocked.contains(name)) {
            action(player, "Ese movimiento aún no está registrado en tu Move Dex.", ChatFormatting.RED);
            return false;
        }
        MoveTemplate move = Moves.INSTANCE.getByName(name);
        if (move == null) {
            action(player, "Movimiento no encontrado: " + rawMove, ChatFormatting.RED);
            return false;
        }
        if (!isNearMachine(player)) {
            action(player, "Debes estar junto a una Máquina de MT.", ChatFormatting.RED);
            return false;
        }

        Item tm = ForgeRegistries.ITEMS.getValue(new ResourceLocation("simpletms", "tm_" + name));
        Item blank = ForgeRegistries.ITEMS.getValue(new ResourceLocation("simpletms", "tm_blank"));
        String type = move.getElementalType() == null ? "normal" : move.getElementalType().getName().toLowerCase(Locale.ROOT);
        Item gem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cobblemon", type + "_gem"));
        if (tm == null || tm == Items.AIR || blank == null || blank == Items.AIR || gem == null || gem == Items.AIR) {
            action(player, "SimpleTMs no tiene una MT válida para ese movimiento.", ChatFormatting.RED);
            return false;
        }

        if (!has(player, blank, 1) || !has(player, gem, 1) || !has(player, Items.REDSTONE, 1)) {
            action(player, "Faltan materiales: MT en blanco + gema " + type + " + redstone.", ChatFormatting.RED);
            return false;
        }
        take(player, blank, 1);
        take(player, gem, 1);
        take(player, Items.REDSTONE, 1);
        ItemStack result = new ItemStack(tm);
        if (!player.getInventory().add(result)) player.drop(result, false);
        player.inventoryMenu.broadcastChanges();
        action(player, "MT creada: " + move.getDisplayName().getString(), ChatFormatting.GREEN);
        return true;
    }

    private static void action(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    public static boolean isNearMachine(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-5, -3, -5), origin.offset(5, 3, 5))) {
            if (player.level().getBlockState(p).is(ModBlocks.TM_MACHINE.get())) return true;
        }
        return false;
    }

    private static boolean hasTMItem(String move) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("simpletms", "tm_" + move));
        return item != null && item != Items.AIR;
    }

    private static Set<String> loadUnlocks(ServerPlayer player) {
        Set<String> out = new HashSet<>();
        ListTag list = player.getPersistentData().getList(KEY_UNLOCKS, Tag.TAG_STRING);
        for (Tag tag : list) out.add(tag.getAsString());
        return out;
    }

    private static void saveUnlocks(ServerPlayer player, Set<String> values) {
        ListTag list = new ListTag();
        values.stream().sorted().forEach(v -> list.add(StringTag.valueOf(v)));
        player.getPersistentData().put(KEY_UNLOCKS, list);
    }

    public static void copyPersistentData(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        CompoundTag old = oldPlayer.getPersistentData();
        CompoundTag fresh = newPlayer.getPersistentData();
        if (old.contains(KEY_UNLOCKS)) fresh.put(KEY_UNLOCKS, old.get(KEY_UNLOCKS).copy());
        if (old.contains(KEY_DEX_PAGE)) fresh.putInt(KEY_DEX_PAGE, old.getInt(KEY_DEX_PAGE));
        if (old.contains(KEY_TM_PAGE)) fresh.putInt(KEY_TM_PAGE, old.getInt(KEY_TM_PAGE));
    }

    private static boolean has(ServerPlayer player, Item item, int amount) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) found += stack.getCount();
            if (found >= amount) return true;
        }
        return false;
    }

    private static void take(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) continue;
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
            if (remaining <= 0) break;
        }
    }

    private TMSystem() {}
}
