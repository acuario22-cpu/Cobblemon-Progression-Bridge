package dev.containersorter.network;

import dev.containersorter.SortMode;
import dev.containersorter.logic.StorageSorter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SortRequest(int containerId, SortMode mode) {
    public static void encode(SortRequest message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId());
        buffer.writeEnum(message.mode());
    }

    public static SortRequest decode(FriendlyByteBuf buffer) {
        return new SortRequest(buffer.readVarInt(), buffer.readEnum(SortMode.class));
    }

    public static void handle(SortRequest message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (player.containerMenu.containerId == message.containerId()) {
                    StorageSorter.sort(player, player.containerMenu, message.mode());
                }
            });
        }
        context.setPacketHandled(true);
    }
}
