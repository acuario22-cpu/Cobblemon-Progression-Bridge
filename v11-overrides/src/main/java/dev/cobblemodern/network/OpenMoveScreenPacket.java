package dev.cobblemodern.network;

import dev.cobblemodern.client.ClientScreenHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OpenMoveScreenPacket(ScreenMode mode, List<MoveEntryData> entries) {
    public enum ScreenMode { MOVE_DEX, TM_MACHINE }

    public static void encode(OpenMoveScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
        buf.writeVarInt(packet.entries.size());
        for (MoveEntryData entry : packet.entries) entry.encode(buf);
    }

    public static OpenMoveScreenPacket decode(FriendlyByteBuf buf) {
        ScreenMode mode = buf.readEnum(ScreenMode.class);
        int size = Math.min(buf.readVarInt(), 2048);
        List<MoveEntryData> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) entries.add(MoveEntryData.decode(buf));
        return new OpenMoveScreenPacket(mode, entries);
    }

    public static void handle(OpenMoveScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientScreenHooks.open(packet)));
        context.setPacketHandled(true);
    }
}
