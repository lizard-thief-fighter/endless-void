package net.voidmod.transform;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/** One stack travelling from one dimension to another. */
public record PortalTransformationInput(ItemStack stack, ResourceKey<Level> from, ResourceKey<Level> to) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
