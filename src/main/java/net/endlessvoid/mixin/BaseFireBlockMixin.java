package net.endlessvoid.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.endlessvoid.portal.PortalLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Fire appearing inside a valid frame (flint and steel, fire charge, dispenser, spreading fire) lights a portal.
 * Both Void and Nether portals are lit by {@link PortalLighting} from their data-driven portal types.
 */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void void$lightPortal(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                  boolean movedByPiston, CallbackInfo ci) {
        if (!oldState.is(state.getBlock()) && PortalLighting.tryLight(level, pos)) {
            ci.cancel();
        }
    }

    /**
     * Vanilla's own Nether lighting is switched off; otherwise it could light frames the nether portal type rejects
     * (anchors, a larger minimum size). The fire then falls through to vanilla's normal survival check.
     */
    @WrapOperation(method = "onPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/portal/PortalShape;findEmptyPortalShape(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction$Axis;)Ljava/util/Optional;"))
    private Optional<PortalShape> void$skipVanillaNetherLighting(LevelAccessor level, BlockPos pos, Direction.Axis axis,
                                                                 Operation<Optional<PortalShape>> original) {
        return Optional.empty();
    }
}
