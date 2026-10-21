package net.voidmod.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.voidmod.registry.VoidBlockEntities;

/** Exists only so the portal can be drawn with the End portal shader by a block entity renderer. */
public class VoidPortalBlockEntity extends BlockEntity {
    public VoidPortalBlockEntity(BlockPos pos, BlockState state) {
        super(VoidBlockEntities.VOID_PORTAL.get(), pos, state);
    }
}
