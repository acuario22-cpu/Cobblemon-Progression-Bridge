package dev.containersorter.client;

import dev.containersorter.ContainerSorterPlus;
import dev.containersorter.SortMode;
import dev.containersorter.logic.StorageSorter;
import dev.containersorter.network.SortRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ContainerSorterPlus.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainerButtons {
    private ContainerButtons() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !StorageSorter.isSupported(screen.getMenu(), minecraft.player.getInventory())) {
            return;
        }

        int totalWidth = 126;
        int x = Math.max(4, (screen.width - totalWidth) / 2);
        int y = 4;

        event.addListener(button(x, y, 38, "A-Z", screen, SortMode.ALPHABETICAL,
                "Ordenar alfabéticamente por ID de objeto"));
        event.addListener(button(x + 42, y, 38, "MOD", screen, SortMode.MOD,
                "Agrupar por mod y luego A-Z"));
        event.addListener(button(x + 84, y, 42, "TIPO", screen, SortMode.TYPE_MOD_ALPHABETICAL,
                "Agrupar por tipo, mod y A-Z"));
    }

    private static Button button(int x, int y, int width, String label,
                                 AbstractContainerScreen<?> screen, SortMode mode, String tooltip) {
        return Button.builder(Component.literal(label), button ->
                        ContainerSorterPlus.NETWORK.sendToServer(new SortRequest(screen.getMenu().containerId, mode)))
                .bounds(x, y, width, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltip)))
                .build();
    }
}
