package net.endlessvoid.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.endlessvoid.VoidDimensions;
import net.endlessvoid.compat.quark.QuarkCompat;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Quark's chorus vegetation (chorus weeds and twist) generate in the Void as well as the End. Its biome and
 * block checks (End Highlands, end stone) already match the Void; its hard-coded dimension list and its exclusion
 * zone around the End's central island (1050 blocks from 0,0) kept it out.
 * {@code @Pseudo}/{@code require = 0}: inactive without Quark, and disabled rather than crashing if Quark changes.
 */
@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.world.gen.ChorusVegetationGenerator")
public abstract class QuarkChorusVegetationMixin {
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void endless_void$allowInVoid(CallbackInfo ci) {
        QuarkCompat.allowGeneratorInVoid(this);
    }

    /** The Void has no central island, and at 1:32 most of its reachable area lies inside that exclusion zone. */
    @ModifyExpressionValue(method = "getSourcesInChunk", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private boolean endless_void$noCentreExclusionInVoid(boolean nearCentre, @Local(argsOnly = true) WorldGenRegion world) {
        return nearCentre && world.getLevel().dimension() != VoidDimensions.THE_VOID;
    }
}
