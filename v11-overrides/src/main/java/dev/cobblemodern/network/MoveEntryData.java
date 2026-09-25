package dev.cobblemodern.network;

import net.minecraft.network.FriendlyByteBuf;

public record MoveEntryData(String id, String name, String type, int power, int accuracy, boolean craftable) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id, 128);
        buf.writeUtf(name, 128);
        buf.writeUtf(type, 64);
        buf.writeVarInt(power);
        buf.writeVarInt(accuracy);
        buf.writeBoolean(craftable);
    }

    public static MoveEntryData decode(FriendlyByteBuf buf) {
        return new MoveEntryData(
            buf.readUtf(128),
            buf.readUtf(128),
            buf.readUtf(64),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean()
        );
    }
}
