package com.hisroyalty.cobbledgacha.client;

import com.hisroyalty.cobbledgacha.item.GeoMachineBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MachineItemRenderer extends GeoItemRenderer<GeoMachineBlockItem> {
    public MachineItemRenderer() {
        super(new MachineItemModel());
    }
}
