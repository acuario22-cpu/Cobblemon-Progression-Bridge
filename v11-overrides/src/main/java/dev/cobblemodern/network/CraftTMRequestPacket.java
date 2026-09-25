package dev.cobblemodern.network;

import dev.cobblemodern.tm.TMSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CraftTMRequestPacket(String moveId) {
    public static void encode(CraftTMRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.moveId, 128);
    }

    public static CraftTMRequestPacket decode(FriendlyByteBuf buf) {
        return new CraftTMRequestPacket(buf.readUtf(128));
    }

    public static void handle(CraftTMRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) context.enqueueWork(() -> TMSystem.craft(sender, packet.moveId));
        context.setPacketHandled(true);
    }
}
