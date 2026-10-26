package net.endlessvoid.mixin.compat;

import net.endlessvoid.compat.quark.QuarkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandles;

/**
 * Makes the Void part of the default configuration of Quark's configurable End features: spiral spires and myalite
 * (stone generation and air clusters). Inactive without Quark; disabled rather than crashing if Quark changes.
 */
@Pseudo
@Mixin(targets = {
        "org.violetmoon.quark.content.world.module.SpiralSpiresModule",
        "org.violetmoon.quark.content.world.module.NewStoneTypesModule",
        "org.violetmoon.quark.content.world.module.BigStoneClustersModule"
})
public abstract class QuarkEndFeatureDefaultsMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"), require = 0)
    private static void endless_void$defaultToVoid(CallbackInfo ci) {
        // This code is merged into the module class, so the lookup class is the module being initialized.
        QuarkCompat.defaultEndFeaturesToVoid(MethodHandles.lookup().lookupClass());
    }
}
