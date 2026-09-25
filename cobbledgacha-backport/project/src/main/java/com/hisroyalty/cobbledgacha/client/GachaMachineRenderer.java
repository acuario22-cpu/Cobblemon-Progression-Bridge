package com.hisroyalty.cobbledgacha.client;
import com.hisroyalty.cobbledgacha.block.GachaMachineBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
public class GachaMachineRenderer extends GeoBlockRenderer<GachaMachineBlockEntity>{
    public GachaMachineRenderer(BlockEntityRendererProvider.Context context){super(new GachaMachineModel());}
}
