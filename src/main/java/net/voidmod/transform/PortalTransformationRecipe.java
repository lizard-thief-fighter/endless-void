package net.voidmod.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.voidmod.registry.VoidRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Turns items into other items when they travel between dimensions, e.g. stone into netherrack on entering the Nether.
 *
 * <pre>{@code
 * {
 *   "type": "void:portal_transformation",
 *   "ingredient": { "item": "minecraft:stone" },   // any ingredient: item, tag, or a list
 *   "result": "minecraft:netherrack",              // stack count and components are kept
 *   "from": "minecraft:overworld",                 // optional; omitted = any source dimension
 *   "to": "minecraft:the_nether",                  // optional; omitted = any destination
 *   "reversible": true                             // optional, default false; needs both from and to
 * }
 * }</pre>
 *
 * A reversible recipe also turns {@code result} back into the ingredient's first item when travelling from
 * {@code to} to {@code from}, so reversible recipes should use a single-item ingredient.
 */
public record PortalTransformationRecipe(Ingredient ingredient, Item result, Optional<ResourceKey<Level>> from,
                                         Optional<ResourceKey<Level>> to, boolean reversible)
        implements Recipe<PortalTransformationInput> {

    private static final Codec<ResourceKey<Level>> DIMENSION = ResourceKey.codec(Registries.DIMENSION);

    public static final MapCodec<PortalTransformationRecipe> CODEC = RecordCodecBuilder.<PortalTransformationRecipe>mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PortalTransformationRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(PortalTransformationRecipe::result),
            DIMENSION.optionalFieldOf("from").forGetter(PortalTransformationRecipe::from),
            DIMENSION.optionalFieldOf("to").forGetter(PortalTransformationRecipe::to),
            Codec.BOOL.optionalFieldOf("reversible", false).forGetter(PortalTransformationRecipe::reversible)
    ).apply(i, PortalTransformationRecipe::new)).validate(PortalTransformationRecipe::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalTransformationRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, PortalTransformationRecipe::ingredient,
            ByteBufCodecs.registry(Registries.ITEM), PortalTransformationRecipe::result,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), PortalTransformationRecipe::from,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), PortalTransformationRecipe::to,
            ByteBufCodecs.BOOL, PortalTransformationRecipe::reversible,
            PortalTransformationRecipe::new);

    private static DataResult<PortalTransformationRecipe> validate(PortalTransformationRecipe recipe) {
        if (recipe.reversible && (recipe.from.isEmpty() || recipe.to.isEmpty())) {
            return DataResult.error(() -> "A reversible portal transformation needs both \"from\" and \"to\"");
        }
        return DataResult.success(recipe);
    }

    /** The item this stack becomes when travelling {@code travelFrom -> travelTo}, or null if this recipe doesn't apply. */
    public @Nullable Item outputFor(ItemStack stack, ResourceKey<Level> travelFrom, ResourceKey<Level> travelTo) {
        if (stack.isEmpty()) return null;
        boolean forward = from.map(travelFrom::equals).orElse(true) && to.map(travelTo::equals).orElse(true);
        if (forward && ingredient.test(stack) && !stack.is(result)) return result;
        if (reversible && from.get().equals(travelTo) && to.get().equals(travelFrom) && stack.is(result)) {
            ItemStack[] options = ingredient.getItems();
            if (options.length > 0 && !options[0].is(result)) return options[0].getItem();
        }
        return null;
    }

    @Override
    public boolean matches(PortalTransformationInput input, Level level) {
        return outputFor(input.stack(), input.from(), input.to()) != null;
    }

    @Override
    public ItemStack assemble(PortalTransformationInput input, HolderLookup.Provider registries) {
        Item output = outputFor(input.stack(), input.from(), input.to());
        return output == null ? ItemStack.EMPTY : new ItemStack(output, input.stack().getCount());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(result);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** Not a crafting-table recipe; keeps it out of the recipe book. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VoidRecipes.PORTAL_TRANSFORMATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return VoidRecipes.PORTAL_TRANSFORMATION.get();
    }

    public static final class Serializer implements RecipeSerializer<PortalTransformationRecipe> {
        @Override
        public MapCodec<PortalTransformationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PortalTransformationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
