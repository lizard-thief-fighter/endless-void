package net.endlessvoid.mixin.compat;

import net.endlessvoid.compat.powergrid.TransformerLinks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Makes breaking one half of a Power Grid interdimensional transformer reliably remove the other half, by tracking
 * the halves Power Grid links ({@link TransformerLinks}) instead of relying only on its portal-geometry lookup.
 * {@code @Pseudo}/{@code require = 0}: inactive without Power Grid, and disabled rather than crashing if it changes.
 */
@Pseudo
@Mixin(targets = "org.patryk3211.powergrid.electricity.transformer.NetherTransformerBlockEntity")
public abstract class PowerGridTransformerLinkMixin {
    @Shadow private UUID id;
    @Shadow private boolean secondary;

    @Inject(method = "link", at = @At("TAIL"), require = 0)
    private void endless_void$recordHalf(UUID id, boolean secondary, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() instanceof ServerLevel level) TransformerLinks.record(level, id, secondary, self.getBlockPos());
    }

    @Inject(method = "destroy", at = @At("TAIL"), require = 0)
    private void endless_void$removeOtherHalf(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (id != null && self.getLevel() instanceof ServerLevel level) {
            TransformerLinks.removeOtherHalf(level, id, secondary, self.getType());
        }
    }
}
