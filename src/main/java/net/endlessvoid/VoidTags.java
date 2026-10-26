package net.endlessvoid;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class VoidTags {
    /** Every block that may form part of a Void portal frame. */
    public static final TagKey<Block> VOID_PORTAL_FRAME = block("void_portal_frame");
    /**
     * Immovable blocks a frame must contain at least one of. Links are keyed by these positions,
     * so a portal keeps its link when its frame is resized.
     */
    public static final TagKey<Block> VOID_PORTAL_ANCHOR = block("void_portal_anchor");
    /** Blocks inside a frame that lighting may replace, e.g. sculk veins in Ancient Cities. */
    public static final TagKey<Block> VOID_PORTAL_REPLACEABLE = block("void_portal_replaceable");
    /** Blocks that can form a Nether portal frame (obsidian by default). */
    public static final TagKey<Block> NETHER_PORTAL_FRAME = block("nether_portal_frame");

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, VoidMod.id(path));
    }

    private VoidTags() {}
}
