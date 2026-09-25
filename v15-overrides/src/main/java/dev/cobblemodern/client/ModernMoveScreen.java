package dev.cobblemodern.client;

import dev.cobblemodern.network.ModernNetwork;
import dev.cobblemodern.network.MoveEntryData;
import dev.cobblemodern.network.OpenMoveScreenPacket;
import dev.cobblemodern.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ModernMoveScreen extends Screen {
    private static final int PANEL_W = 352;
    private static final int PANEL_H = 286;

    private final OpenMoveScreenPacket.ScreenMode mode;
    private final List<MoveEntryData> entries;
    private final int page;

    public ModernMoveScreen(OpenMoveScreenPacket.ScreenMode mode, List<MoveEntryData> entries, int page) {
        super(Component.literal(mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? "Máquina de MT" : "Move Dex"));
        this.mode = mode;
        this.entries = List.copyOf(entries);
        this.page = Math.floorMod(page, pageCount());
    }

    private int pageSize() {
        return mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? 7 : 8;
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + pageSize() - 1) / pageSize());
    }

    private int rowStartY(int top) {
        return mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? top + 72 : top + 52;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int pages = pageCount();

        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b ->
                minecraft.setScreen(new ModernMoveScreen(mode, entries, page - 1)))
                .bounds(left + 12, top + PANEL_H - 30, 28, 20).build());

            addRenderableWidget(Button.builder(Component.literal(">"), b ->
                minecraft.setScreen(new ModernMoveScreen(mode, entries, page + 1)))
                .bounds(left + 46, top + PANEL_H - 30, 28, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
            .bounds(left + PANEL_W - 70, top + PANEL_H - 30, 58, 20).build());

        if (mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE) {
            int start = page * pageSize();
            int end = Math.min(entries.size(), start + pageSize());

            for (int i = start; i < end; i++) {
                MoveEntryData entry = entries.get(i);
                int row = i - start;
                int y = rowStartY(top) + row * 20;

                Component label = Component.literal("Fabricar").withStyle(
                    entry.hasMaterials() ? ChatFormatting.GREEN : ChatFormatting.RED
                );

                addRenderableWidget(Button.builder(label, b -> {
                    if (entry.hasMaterials()) {
                        ModernNetwork.requestCraft(entry.id());
                    }
                }).bounds(left + PANEL_W - 75, y, 62, 18).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;

        graphics.fill(left, top, right, bottom, 0xF20B111B);
        graphics.fill(left + 2, top + 2, right - 2, top + 34,
            mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? 0xFFB36A1B : 0xFF147E9B);
        graphics.fill(left + 4, top + 36, right - 4, bottom - 64, 0xE9162230);
        graphics.fill(left + 4, bottom - 62, right - 4, bottom - 4, 0xFF101923);

        ItemStack icon = new ItemStack(
            mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE
                ? ModItems.TM_MACHINE.get()
                : ModItems.MOVE_DEX.get()
        );

        graphics.renderItem(icon, left + 10, top + 9);
        graphics.drawString(font, title, left + 34, top + 13, 0xFFFFFFFF, true);

        String pageText = (page + 1) + "/" + pageCount();
        graphics.drawString(font, Component.literal(pageText), right - 38, top + 13, 0xFFFFFFFF, true);

        graphics.drawString(font,
            Component.literal(mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE
                ? "Movimientos desbloqueados con MT disponible"
                : "Movimientos registrados por tu equipo y PC"),
            left + 12, top + 38, 0xFFB7C8D8, false);

        if (mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE) {
            graphics.drawString(font, Component.literal("Coste por MT:"), left + 12, top + 50, 0xFFE9C37A, false);
            graphics.drawString(font,
                Component.literal("1 MT en blanco + 1 gema del tipo + 1 redstone"),
                left + 78, top + 50, 0xFFFFFFFF, false);
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font,
                Component.literal(mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE
                    ? "Aún no hay MT desbloqueadas."
                    : "Aún no hay movimientos registrados."),
                left + PANEL_W / 2, top + 122, 0xFFBFC8D2);
        } else {
            int start = page * pageSize();
            int end = Math.min(entries.size(), start + pageSize());
            int rowStart = rowStartY(top);

            for (int i = start; i < end; i++) {
                MoveEntryData entry = entries.get(i);
                int row = i - start;
                int y = rowStart + row * 20;
                int rowRight = mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? right - 82 : right - 12;

                graphics.fill(left + 10, y, rowRight, y + 18,
                    (row % 2 == 0) ? 0xA5263444 : 0xA51E2B39);

                graphics.fill(left + 12, y + 2, left + 16, y + 16, typeColor(entry.type()));
                graphics.drawString(font, Component.literal(entry.name()), left + 21, y + 5, 0xFFFFFFFF, false);

                String stats = "[" + entry.type() + "]  P:" + stat(entry.power()) + "  Acc:" + stat(entry.accuracy());
                int statsX = mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE ? left + 158 : left + 188;
                graphics.drawString(font, Component.literal(stats), statsX, y + 5, 0xFFB5C3D1, false);
            }
        }

        if (mode == OpenMoveScreenPacket.ScreenMode.TM_MACHINE) {
            graphics.drawCenteredString(font,
                Component.literal("Verde = listo para fabricar"),
                left + PANEL_W / 2, bottom - 56, 0xFF7EE081);
            graphics.drawCenteredString(font,
                Component.literal("Rojo = faltan materiales"),
                left + PANEL_W / 2, bottom - 44, 0xFFE26B6B);
        } else {
            graphics.drawCenteredString(font,
                Component.literal("Los movimientos descubiertos"),
                left + PANEL_W / 2, bottom - 56, 0xFF8ECADB);
            graphics.drawCenteredString(font,
                Component.literal("quedan registrados permanentemente."),
                left + PANEL_W / 2, bottom - 44, 0xFF8ECADB);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String stat(int value) {
        return value <= 0 ? "—" : Integer.toString(value);
    }

    private static int typeColor(String type) {
        return switch (type.toLowerCase()) {
            case "fire" -> 0xFFF08030;
            case "water" -> 0xFF6890F0;
            case "grass" -> 0xFF78C850;
            case "electric" -> 0xFFF8D030;
            case "ice" -> 0xFF98D8D8;
            case "fighting" -> 0xFFC03028;
            case "poison" -> 0xFFA040A0;
            case "ground" -> 0xFFE0C068;
            case "flying" -> 0xFFA890F0;
            case "psychic" -> 0xFFF85888;
            case "bug" -> 0xFFA8B820;
            case "rock" -> 0xFFB8A038;
            case "ghost" -> 0xFF705898;
            case "dragon" -> 0xFF7038F8;
            case "dark" -> 0xFF705848;
            case "steel" -> 0xFFB8B8D0;
            case "fairy" -> 0xFFEE99AC;
            default -> 0xFFA8A878;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
