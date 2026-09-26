package dev.mrshawn.pokeblocks.block;

import dev.mrshawn.pokeblocks.PokeblocksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DollBlock extends BaseEntityBlock {
    private final String dollId;
    private final boolean gigantic;

    public DollBlock(Properties properties, String dollId, boolean gigantic) {
        super(properties);
        this.dollId = dollId;
        this.gigantic = gigantic;
    }

    public String dollId() {
        return dollId;
    }

    public boolean gigantic() {
        return gigantic;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DollBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        return null;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        // Gigantic dolls are rendered at exactly 2x scale around the block centre.
        // Keep the selection/collision volume aligned with that visual size instead
        // of leaving them with the normal one-block hitbox.
        return gigantic
            ? box(-8, 0, -8, 24, 32, 24)
            : box(3, 0, 3, 13, 11, 13);
    }

    @Override
    public VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return getShape(state, level, pos, context);
    }
}
