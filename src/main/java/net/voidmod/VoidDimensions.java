package net.voidmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class VoidDimensions {
    public static final ResourceKey<Level> THE_VOID = ResourceKey.create(Registries.DIMENSION, VoidMod.id("the_void"));

    private VoidDimensions() {}
}
