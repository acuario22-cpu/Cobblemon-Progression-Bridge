package dev.mrshawn.pokeblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.mrshawn.pokeblocks.block.DollBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DollBlockRenderer extends GeoBlockRenderer<DollBlockEntity> {
    private static final float GIGANTIC_WORLD_SCALE = 2.0F;

    public DollBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DollBlockModel());
    }

    @Override
    public void preRender(
        PoseStack poseStack,
        DollBlockEntity animatable,
        BakedGeoModel model,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight,
        int packedOverlay,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        if (animatable.gigantic()) {
            // Scale around the centre of the placement block so every Gigantic
            // and Gigantic Shiny doll uses the original 2x world presentation.
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.scale(
                GIGANTIC_WORLD_SCALE,
                GIGANTIC_WORLD_SCALE,
                GIGANTIC_WORLD_SCALE
            );
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }

        super.preRender(
            poseStack,
            animatable,
            model,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            red,
            green,
            blue,
            alpha
        );
    }
}
