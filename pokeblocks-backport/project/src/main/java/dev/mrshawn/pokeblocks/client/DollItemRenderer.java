package dev.mrshawn.pokeblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mrshawn.pokeblocks.item.DollBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DollItemRenderer extends GeoItemRenderer<DollBlockItem> {
    // The original Pokeblocks presentation uses a larger Gigantic preview:
    // normal GUI ~= 0.50, gigantic GUI ~= 0.70  -> relative 1.40x
    // normal hand/ground ~= 0.50, gigantic ~= 0.75 -> relative 1.50x
    private static final float GIGANTIC_GUI_SCALE = 1.40F;
    private static final float GIGANTIC_OTHER_SCALE = 1.50F;

    public DollItemRenderer() {
        super(new DollItemModel());
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();

        if (stack.getItem() instanceof DollBlockItem doll && doll.gigantic()) {
            float scale = displayContext == ItemDisplayContext.GUI
                ? GIGANTIC_GUI_SCALE
                : GIGANTIC_OTHER_SCALE;

            poseStack.scale(scale, scale, scale);
        }

        super.renderByItem(
            stack,
            displayContext,
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay
        );

        poseStack.popPose();
    }
}
