package net.voidmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.voidmod.portal.VoidPortals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lights Void portals the way vanilla lights Nether portals: fire appearing inside a valid frame
 * (flint and steel, fire charge, dispenser, spreading fire) turns the opening into portal blocks.
 */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void void$lightVoidPortal(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                      boolean movedByPiston, CallbackInfo ci) {
        if (!oldState.is(state.getBlock()) && VoidPortals.tryLight(level, pos)) {
            ci.cancel();
        }
    }
}
