package net.endlessvoid.mixin.compat;

import net.endlessvoid.compat.quark.QuarkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/** Exposes the dimension list of Zeta's (Quark's library) DimensionConfig. Inactive without Zeta. */
@Pseudo
@Mixin(targets = "org.violetmoon.zeta.config.type.DimensionConfig")
public abstract class ZetaDimensionConfigMixin implements QuarkCompat.DimensionList {
    @Shadow private List<String> dimensions;

    @Override
    public List<String> endless_void$dimensions() {
        return dimensions;
    }
}
