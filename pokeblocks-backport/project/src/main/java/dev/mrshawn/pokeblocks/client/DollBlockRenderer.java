package dev.mrshawn.pokeblocks.client;

import dev.mrshawn.pokeblocks.block.DollBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DollBlockRenderer extends GeoBlockRenderer<DollBlockEntity> {
    public DollBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DollBlockModel());
    }
}
