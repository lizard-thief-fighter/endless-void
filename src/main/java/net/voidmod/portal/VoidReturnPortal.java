package net.voidmod.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.voidmod.VoidMod;
import net.voidmod.VoidTags;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Places the Void-side return portal from the structure template {@code data/void/structure/void_return_portal.nbt},
 * then lights it. Any template works if it contains a valid frame with an anchor block, built along the X axis;
 * it is rotated to match the source portal.
 */
public final class VoidReturnPortal {
    public static final ResourceLocation TEMPLATE = VoidMod.id("void_return_portal");
    private static final int SEARCH_RADIUS = 16;
    /** Height used when no ground is found within the search radius (the portal floats on its platform). */
    private static final int FLOATING_Y = 64;

    public static @Nullable VoidPortalShape place(ServerLevel level, BlockPos target, Direction.Axis axis) {
        Optional<StructureTemplate> found = level.getStructureManager().get(TEMPLATE);
        if (found.isEmpty()) {
            VoidMod.LOGGER.error("Missing structure template {}", TEMPLATE);
            return null;
        }
        StructureTemplate template = found.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(axis == Direction.Axis.X ? Rotation.NONE : Rotation.CLOCKWISE_90)
                .setIgnoreEntities(true);

        BoundingBox local = template.getBoundingBox(settings, BlockPos.ZERO);
        BlockPos corner = findCorner(level, target, local.getXSpan(), local.getYSpan(), local.getZSpan());
        BlockPos origin = corner.offset(-local.minX(), -local.minY(), -local.minZ());
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);

        BoundingBox placed = template.getBoundingBox(settings, origin);
        for (BlockPos pos : BlockPos.betweenClosed(placed.minX(), placed.minY(), placed.minZ(),
                placed.maxX(), placed.maxY(), placed.maxZ())) {
            if (!level.getBlockState(pos).is(VoidTags.VOID_PORTAL_ANCHOR)) continue;
            Optional<VoidPortalShape> shape = VoidPortalShape.findAroundAnchor(level, pos.immutable(), axis);
            if (shape.isPresent()) {
                shape.get().light(level);
                return shape.get();
            }
        }
        VoidMod.LOGGER.error("Structure template {} has no valid portal frame with an anchor block", TEMPLATE);
        return null;
    }

    /**
     * Min corner for the template: the nearest column with ground (within {@link #SEARCH_RADIUS}) so the
     * template's bottom layer replaces the surface, or a floating position if the area is empty void.
     */
    private static BlockPos findCorner(ServerLevel level, BlockPos target, int sizeX, int sizeY, int sizeZ) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - sizeY;
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring r only
                    int x = target.getX() + dx;
                    int z = target.getZ() + dz;
                    int surface = VoidPortals.surfaceY(level, x, z);
                    if (surface > minY && surface - 1 <= maxY) {
                        return new BlockPos(x - sizeX / 2, surface - 1, z - sizeZ / 2);
                    }
                }
            }
        }
        return new BlockPos(target.getX() - sizeX / 2, Mth.clamp(FLOATING_Y, minY, maxY), target.getZ() - sizeZ / 2);
    }

    private VoidReturnPortal() {}
}
