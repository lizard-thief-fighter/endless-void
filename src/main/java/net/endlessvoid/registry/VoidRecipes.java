package net.endlessvoid.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.endlessvoid.VoidMod;
import net.endlessvoid.transform.PortalTransformationRecipe;

public final class VoidRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, VoidMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, VoidMod.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<PortalTransformationRecipe>> PORTAL_TRANSFORMATION =
            RECIPE_TYPES.register("portal_transformation", () -> RecipeType.simple(VoidMod.id("portal_transformation")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PortalTransformationRecipe>> PORTAL_TRANSFORMATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("portal_transformation", PortalTransformationRecipe.Serializer::new);

    private VoidRecipes() {}
}
