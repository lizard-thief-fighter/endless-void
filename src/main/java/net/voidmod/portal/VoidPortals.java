package net.voidmod.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.voidmod.VoidDimensions;
import net.voidmod.VoidMod;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Travel rules for Void portals. */
public final class VoidPortals {

    /**
     * Overworld -> Void: follow the stored link, or generate a return portal at the scaled position and link it.
     * Void -> Overworld: follow the stored link back to the Ancient City frame it came from.
     */
    public static @Nullable DimensionTransition findDestination(ServerLevel from, Entity entity, BlockPos portalPos) {
        if (!isPortalDimension(from.dimension())) return null;
        ResourceKey<Level> toKey = from.dimension() == VoidDimensions.THE_VOID ? Level.OVERWORLD : VoidDimensions.THE_VOID;
        ServerLevel to = from.getServer().getLevel(toKey);
        if (to == null) {
            VoidMod.LOGGER.warn("Dimension {} is not loaded; check that its dimension JSON is present", toKey.location());
            return null;
        }

        BlockState state = from.getBlockState(portalPos);
        Direction.Axis axis = state.hasProperty(VoidPortalBlock.AXIS) ? state.getValue(VoidPortalBlock.AXIS) : Direction.Axis.X;
        Optional<PortalFrame> source = PortalFrame.findOnAxis(from, portalPos, axis, PortalSpec.voidPortal(from));
        if (source.isEmpty()) return null;
        PortalFrame src = source.get();

        VoidPortalLinks links = VoidPortalLinks.get(from.getServer());
        GlobalPos linked = links.find(from.dimension(), src.anchors());
        if (linked != null && linked.dimension() == toKey) {
            PortalSpec targetSpec = PortalSpec.voidPortal(to);
            Optional<PortalFrame> target = PortalFrame.findAroundAnchor(to, linked.pos(), src.axis(), targetSpec);
            if (target.isPresent()) {
                if (!target.get().isLit(to, targetSpec)) target.get().light(to, targetSpec);
                return transition(to, entity, target.get().arrivalPoint());
            }
            VoidMod.LOGGER.warn("Linked portal frame at {} in {} is no longer valid", linked.pos(), toKey.location());
            return transition(to, entity, Vec3.atBottomCenterOf(linked.pos().above()));
        }

        BlockPos scaled = scaledPosition(from, to, entity);
        if (toKey == VoidDimensions.THE_VOID) {
            PortalFrame created = VoidReturnPortal.place(to, scaled, src.axis());
            if (created == null) return null;
            links.link(from.dimension(), src, toKey, created);
            return transition(to, entity, created.arrivalPoint());
        }

        // An unlinked portal in the Void (e.g. built with commands). Arrivals in the Overworld need an
        // Ancient City frame, so no portal is generated; the entity is set down on the surface instead.
        int y = surfaceY(to, scaled.getX(), scaled.getZ());
        return transition(to, entity, Vec3.atBottomCenterOf(new BlockPos(scaled.getX(), y, scaled.getZ())));
    }

    static boolean isPortalDimension(ResourceKey<Level> dimension) {
        return dimension == Level.OVERWORLD || dimension == VoidDimensions.THE_VOID;
    }

    /** First empty Y above the highest motion-blocking block, loading the chunk if needed. Returns min build height for empty columns. */
    static int surfaceY(ServerLevel level, int x, int z) {
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
    }

    /** Uses the dimension types' coordinate_scale, so the 1:32 ratio is tuned in the dimension_type JSON. */
    private static BlockPos scaledPosition(ServerLevel from, ServerLevel to, Entity entity) {
        double scale = DimensionType.getTeleportationScale(from.dimensionType(), to.dimensionType());
        return to.getWorldBorder().clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);
    }

    private static DimensionTransition transition(ServerLevel to, Entity entity, Vec3 pos) {
        return new DimensionTransition(to, pos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    private VoidPortals() {}
}
