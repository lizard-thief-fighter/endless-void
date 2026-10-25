package net.voidmod.transform;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.voidmod.registry.VoidRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies portal transformation recipes for one trip ({@code from -> to}). Public so integrations (e.g. Create
 * contraption storage) can convert their own inventories with the same rules.
 */
public final class PortalTransformer {
    /** Shulker boxes and other items with container contents are converted recursively, up to this depth. */
    private static final int MAX_DEPTH = 4;

    private final List<RecipeHolder<PortalTransformationRecipe>> recipes;
    private final ResourceKey<Level> from;
    private final ResourceKey<Level> to;

    private PortalTransformer(List<RecipeHolder<PortalTransformationRecipe>> recipes, ResourceKey<Level> from, ResourceKey<Level> to) {
        this.recipes = recipes;
        this.from = from;
        this.to = to;
    }

    /** Null when nothing can change on this trip (same dimension, or no recipes loaded). */
    public static @Nullable PortalTransformer create(ServerLevel level, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (from.equals(to)) return null;
        List<RecipeHolder<PortalTransformationRecipe>> recipes =
                level.getRecipeManager().getAllRecipesFor(VoidRecipes.PORTAL_TRANSFORMATION.get());
        return recipes.isEmpty() ? null : new PortalTransformer(recipes, from, to);
    }

    /**
     * Converts one stack. Returns null if nothing changed; otherwise the first element replaces the stack and any
     * further elements are overflow (when the result stacks smaller than the input, or from nested contents).
     */
    public @Nullable List<ItemStack> transform(ItemStack stack) {
        return transform(stack, 0);
    }

    private @Nullable List<ItemStack> transform(ItemStack stack, int depth) {
        if (stack.isEmpty()) return null;
        List<ItemStack> overflow = new ArrayList<>();
        ItemStack working = stack;

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && depth < MAX_DEPTH) {
            List<ItemStack> inner = new ArrayList<>();
            boolean changed = false;
            for (ItemStack content : contents.stream().toList()) {
                List<ItemStack> result = transform(content, depth + 1);
                if (result == null) {
                    inner.add(content);
                } else {
                    changed = true;
                    inner.add(result.getFirst());
                    overflow.addAll(result.subList(1, result.size()));
                }
            }
            if (changed) {
                working = stack.copy();
                working.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inner));
            }
        }

        Item output = outputFor(working);
        if (output == null && working == stack) return null;
        List<ItemStack> result = output == null ? new ArrayList<>(List.of(working)) : convert(working, output);
        result.addAll(overflow);
        return result;
    }

    private @Nullable Item outputFor(ItemStack stack) {
        for (RecipeHolder<PortalTransformationRecipe> holder : recipes) {
            Item output = holder.value().outputFor(stack, from, to);
            if (output != null) return output;
        }
        return null;
    }

    /** Same count and components on the new item, split into stacks if the new item stacks smaller. */
    private static List<ItemStack> convert(ItemStack stack, Item output) {
        ItemStack template = new ItemStack(output.builtInRegistryHolder(), 1, stack.getComponentsPatch());
        int max = Math.max(1, template.getMaxStackSize());
        List<ItemStack> result = new ArrayList<>();
        for (int remaining = stack.getCount(); remaining > 0; remaining -= max) {
            result.add(template.copyWithCount(Math.min(max, remaining)));
        }
        return result;
    }
}
