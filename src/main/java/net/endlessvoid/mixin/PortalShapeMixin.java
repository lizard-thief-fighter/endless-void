package net.endlessvoid.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.portal.PortalShape;
import net.endlessvoid.portal.PortalTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps vanilla's Nether frame check (obsidian, or NeoForge's isPortalFrame) for the data-driven nether portal type.
 * Vanilla still uses PortalShape to decide when a Nether portal breaks and where fire may be placed, so this keeps
 * portals in custom frames from collapsing on the next block update.
 */
@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {
    @Shadow @Final @Mutable
    private static BlockBehaviour.StatePredicate FRAME;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void void$dataDrivenFrame(CallbackInfo ci) {
        FRAME = PortalTypes::isNetherFrame;
    }
}
