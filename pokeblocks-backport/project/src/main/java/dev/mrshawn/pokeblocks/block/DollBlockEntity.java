package dev.mrshawn.pokeblocks.block;

import dev.mrshawn.pokeblocks.PokeblocksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DollBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DollBlockEntity(BlockPos pos, BlockState state) {
        super(PokeblocksForge.DOLL_BE.get(), pos, state);
    }

    public String dollId() {
        return getBlockState().getBlock() instanceof DollBlock doll
            ? doll.dollId()
            : "";
    }

    public boolean gigantic() {
        return getBlockState().getBlock() instanceof DollBlock doll && doll.gigantic();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "static", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
