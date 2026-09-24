package dev.zanckor.cobblemonrider.network.packet;

import com.google.gson.Gson;
import dev.zanckor.cobblemonrider.config.PokemonJsonObject;
import dev.zanckor.cobblemonrider.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ConfigPacket {
    private static final Gson GSON = new Gson();

    private static final int FORMAT_VERSION = 2;
    private static final int MAX_UNCOMPRESSED_BYTES = 8 * 1024 * 1024;
    private static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;

    private static final Object CACHE_LOCK = new Object();
    private static PokemonJsonObject cachedObject;
    private static EncodedConfig cachedEncoded;

    private final PokemonJsonObject jsonObject;

    public ConfigPacket(PokemonJsonObject config) {
        this.jsonObject = config;
    }

    public ConfigPacket(FriendlyByteBuf buffer) {
        int formatVersion = buffer.readVarInt();
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported CobblemonRider config packet format: " + formatVersion);
        }

        int expectedLength = readBoundedLength(buffer.readVarInt(), MAX_UNCOMPRESSED_BYTES, "uncompressed");
        int compressedLength = readBoundedLength(buffer.readVarInt(), MAX_COMPRESSED_BYTES, "compressed");

        if (compressedLength > buffer.readableBytes()) {
            throw new IllegalArgumentException(
                    "CobblemonRider config packet is truncated: expected " + compressedLength
                            + " bytes, got " + buffer.readableBytes());
        }

        byte[] compressed = new byte[compressedLength];
        buffer.readBytes(compressed);

        byte[] raw = decompress(compressed, expectedLength);
        this.jsonObject = GSON.fromJson(new String(raw, StandardCharsets.UTF_8), PokemonJsonObject.class);

        if (this.jsonObject == null) {
            throw new IllegalArgumentException("CobblemonRider config decoded to null");
        }
    }

    public void encodeBuffer(FriendlyByteBuf buffer) {
        EncodedConfig encoded = getOrEncode(this.jsonObject);

        buffer.writeVarInt(FORMAT_VERSION);
        buffer.writeVarInt(encoded.rawLength);
        buffer.writeVarInt(encoded.compressed.length);
        buffer.writeBytes(encoded.compressed);
    }

    public static void handler(ConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientHandler.saveConfigObject(msg.jsonObject)
            );
        });
    }

    private static EncodedConfig getOrEncode(PokemonJsonObject config) {
        synchronized (CACHE_LOCK) {
            if (cachedObject == config && cachedEncoded != null) {
                return cachedEncoded;
            }

            byte[] raw = GSON.toJson(config).getBytes(StandardCharsets.UTF_8);
            if (raw.length > MAX_UNCOMPRESSED_BYTES) {
                throw new IllegalArgumentException(
                        "CobblemonRider config is too large: " + raw.length
                                + " bytes (limit " + MAX_UNCOMPRESSED_BYTES + ")");
            }

            byte[] compressed = compress(raw);
            if (compressed.length > MAX_COMPRESSED_BYTES) {
                throw new IllegalArgumentException(
                        "Compressed CobblemonRider config is too large: " + compressed.length
                                + " bytes (limit " + MAX_COMPRESSED_BYTES + ")");
            }

            cachedObject = config;
            cachedEncoded = new EncodedConfig(raw.length, compressed);
            return cachedEncoded;
        }
    }

    private static int readBoundedLength(int value, int max, String label) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException(
                    "Invalid CobblemonRider " + label + " config length: " + value + " (max " + max + ")");
        }
        return value;
    }

    private static byte[] compress(byte[] raw) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(512, raw.length / 4));
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(raw);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to compress CobblemonRider config", e);
        }
    }

    private static byte[] decompress(byte[] compressed, int expectedLength) {
        try {
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream(Math.max(512, Math.min(expectedLength, 64 * 1024)));

            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                byte[] work = new byte[8192];
                int total = 0;
                int read;

                while ((read = gzip.read(work)) != -1) {
                    total += read;
                    if (total > MAX_UNCOMPRESSED_BYTES) {
                        throw new IllegalArgumentException(
                                "Decompressed CobblemonRider config exceeds "
                                        + MAX_UNCOMPRESSED_BYTES + " bytes");
                    }
                    output.write(work, 0, read);
                }
            }

            byte[] raw = output.toByteArray();
            if (raw.length != expectedLength) {
                throw new IllegalArgumentException(
                        "CobblemonRider config length mismatch: expected "
                                + expectedLength + ", decoded " + raw.length);
            }
            return raw;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to decompress CobblemonRider config", e);
        }
    }

    private static final class EncodedConfig {
        private final int rawLength;
        private final byte[] compressed;

        private EncodedConfig(int rawLength, byte[] compressed) {
            this.rawLength = rawLength;
            this.compressed = compressed;
        }
    }
}
