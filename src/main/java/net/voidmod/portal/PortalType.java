package net.voidmod.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.voidmod.VoidTags;

import java.util.Optional;

/**
 * Data-driven materials and size limits for one kind of portal, loaded from
 * {@code data/<namespace>/void/portal_type/<name>.json}. Block fields accept a block id, a list of ids, or a #tag.
 *
 * @param frame          blocks that can form the frame
 * @param anchor         if present, the frame must contain at least one of these blocks
 * @param replaceable    blocks inside the frame that lighting may replace (besides air and fire)
 * @param generatedFrame block used when the game builds an arrival frame (Nether only; the Void uses a structure template)
 */
public record PortalType(HolderSet<Block> frame, Optional<HolderSet<Block>> anchor, HolderSet<Block> replaceable,
                         Optional<Block> generatedFrame, int minWidth, int maxWidth, int minHeight, int maxHeight) {

    private static final Codec<Integer> SIZE = Codec.intRange(1, 64);
    private static final Codec<HolderSet<Block>> BLOCKS = RegistryCodecs.homogeneousList(Registries.BLOCK);

    public static final Codec<PortalType> CODEC = RecordCodecBuilder.<PortalType>create(i -> i.group(
            BLOCKS.fieldOf("frame").forGetter(PortalType::frame),
            BLOCKS.optionalFieldOf("anchor").forGetter(PortalType::anchor),
            BLOCKS.optionalFieldOf("replaceable", HolderSet.<Block>empty()).forGetter(PortalType::replaceable),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("generated_frame").forGetter(PortalType::generatedFrame),
            SIZE.optionalFieldOf("min_width", 2).forGetter(PortalType::minWidth),
            SIZE.optionalFieldOf("max_width", 21).forGetter(PortalType::maxWidth),
            SIZE.optionalFieldOf("min_height", 3).forGetter(PortalType::minHeight),
            SIZE.optionalFieldOf("max_height", 21).forGetter(PortalType::maxHeight)
    ).apply(i, PortalType::new)).validate(PortalType::validate);

    private static DataResult<PortalType> validate(PortalType type) {
        if (type.minWidth > type.maxWidth || type.minHeight > type.maxHeight) {
            return DataResult.error(() -> "Portal type minimum size is larger than its maximum size");
        }
        return DataResult.success(type);
    }

    /** Used only if a datapack breaks the built-in void.json; mirrors it. */
    static PortalType defaultVoid() {
        return new PortalType(tag(VoidTags.VOID_PORTAL_FRAME), Optional.of(tag(VoidTags.VOID_PORTAL_ANCHOR)),
                tag(VoidTags.VOID_PORTAL_REPLACEABLE), Optional.empty(), 2, 21, 3, 21);
    }

    /** Used only if a datapack breaks the built-in nether.json; mirrors it. */
    static PortalType defaultNether() {
        return new PortalType(tag(VoidTags.NETHER_PORTAL_FRAME), Optional.empty(), HolderSet.empty(),
                Optional.of(Blocks.OBSIDIAN), 2, 21, 3, 21);
    }

    private static HolderSet<Block> tag(TagKey<Block> key) {
        return BuiltInRegistries.BLOCK.getOrCreateTag(key);
    }
}
