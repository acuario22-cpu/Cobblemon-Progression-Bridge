package dev.cobblemodern.client;

import dev.cobblemodern.network.OpenMoveScreenPacket;
import net.minecraft.client.Minecraft;

public final class ClientScreenHooks {
    public static void open(OpenMoveScreenPacket packet) {
        Minecraft.getInstance().setScreen(new ModernMoveScreen(packet.mode(), packet.entries(), 0));
    }

    private ClientScreenHooks() {}
}
