package dev.mrshawn.pokeblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mrshawn.pokeblocks.block.DollBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DollBlockRenderer extends GeoBlockRenderer<DollBlockEntity> {
    private static final float GIGANTIC_WORLD_SCALE = 2.0F;

    public DollBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DollBlockModel());
    }

    @Override
    public void render(
        DollBlockEntity animatable,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();

        if (animatable.gigantic()) {
            // Scale around the horizontal centre of the placed block while keeping
            // the doll's feet on the same Y=0 floor as the normal PokéDoll.
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.scale(
                GIGANTIC_WORLD_SCALE,
                GIGANTIC_WORLD_SCALE,
                GIGANTIC_WORLD_SCALE
            );
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }

        super.render(
            animatable,
            partialTick,
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay
        );

        poseStack.popPose();
    }
}
