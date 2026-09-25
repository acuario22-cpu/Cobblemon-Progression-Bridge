package dev.mrshawn.pokeblocks.client;

import dev.mrshawn.pokeblocks.item.DollBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DollItemRenderer extends GeoItemRenderer<DollBlockItem> {
    public DollItemRenderer() {
        super(new DollItemModel());
    }
}
