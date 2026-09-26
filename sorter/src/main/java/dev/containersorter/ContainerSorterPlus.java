package dev.containersorter;

import dev.containersorter.network.SortRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(ContainerSorterPlus.MOD_ID)
public final class ContainerSorterPlus {
    public static final String MOD_ID = "container_sorter_plus";
    private static final String PROTOCOL = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    public ContainerSorterPlus() {
        NETWORK.registerMessage(
                0,
                SortRequest.class,
                SortRequest::encode,
                SortRequest::decode,
                SortRequest::handle
        );
    }
}
