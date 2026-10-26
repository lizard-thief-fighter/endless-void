package net.endlessvoid.mixin.compat;

import net.endlessvoid.compat.quark.QuarkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Quark's chorus vegetation (chorus weeds and twist) generate in the Void as well as the End. Its biome and
 * block checks (End Highlands, end stone) already match the Void; only its hard-coded dimension list excluded it.
 * {@code @Pseudo}/{@code require = 0}: inactive without Quark, and disabled rather than crashing if Quark changes.
 */
@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.world.gen.ChorusVegetationGenerator")
public abstract class QuarkChorusVegetationMixin {
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void endless_void$allowInVoid(CallbackInfo ci) {
        QuarkCompat.allowGeneratorInVoid(this);
    }
}
