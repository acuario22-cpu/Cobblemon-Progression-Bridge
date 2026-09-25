package dev.cobblemodern.network;

import dev.cobblemodern.ModernBackport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Optional;

public final class ModernNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ModernBackport.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, OpenMoveScreenPacket.class,
            OpenMoveScreenPacket::encode,
            OpenMoveScreenPacket::decode,
            OpenMoveScreenPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, CraftTMRequestPacket.class,
            CraftTMRequestPacket::encode,
            CraftTMRequestPacket::decode,
            CraftTMRequestPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void open(ServerPlayer player, OpenMoveScreenPacket.ScreenMode mode, List<MoveEntryData> entries) {
        CHANNEL.sendTo(new OpenMoveScreenPacket(mode, entries), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void requestCraft(String moveId) {
        CHANNEL.sendToServer(new CraftTMRequestPacket(moveId));
    }

    private ModernNetwork() {}
}
