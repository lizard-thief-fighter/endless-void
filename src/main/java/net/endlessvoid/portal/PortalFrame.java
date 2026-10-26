package net.endlessvoid.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A rectangular frame around an opening, validated against a {@link PortalSpec}. Corners are optional, as with
 * vanilla Nether portals. If the spec has an anchor set, the frame must contain at least one anchor block.
 *
 * @param bottomLeft lowest interior cell on the negative side of the axis (west for X, north for Z)
 * @param anchors    anchor blocks in the frame, ordered bottom row first, then top row, then sides
 */
public record PortalFrame(Direction.Axis axis, BlockPos bottomLeft, int width, int height, List<BlockPos> anchors) {

    /** Tries the preferred axis first, then the other, like vanilla's empty portal search. */
    public static Optional<PortalFrame> find(LevelAccessor level, BlockPos inside, Direction.Axis preferred, PortalSpec spec) {
        Optional<PortalFrame> frame = findOnAxis(level, inside, preferred, spec);
        return frame.isPresent() ? frame : findOnAxis(level, inside, other(preferred), spec);
    }

    public static Optional<PortalFrame> findOnAxis(LevelAccessor level, BlockPos inside, Direction.Axis axis, PortalSpec spec) {
        if (!spec.isInterior(level.getBlockState(inside))) return Optional.empty();
        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction right = left.getOpposite();

        // Drop to the floor of the opening.
        BlockPos pos = inside;
        for (int i = 0; i < spec.maxHeight() && spec.isInterior(level.getBlockState(pos.below())); i++) pos = pos.below();
        if (!isFrame(level, pos.below(), spec)) return Optional.empty();

        // Walk to the left edge along the floor.
        for (int i = 0; i < spec.maxWidth() && spec.isInterior(level.getBlockState(pos.relative(left))); i++) pos = pos.relative(left);
        if (!isFrame(level, pos.relative(left), spec)) return Optional.empty();
        BlockPos bottomLeft = pos;

        // Width: every floor cell must sit on frame.
        int width = 0;
        while (width <= spec.maxWidth()) {
            BlockPos cell = bottomLeft.relative(right, width);
            if (!spec.isInterior(level.getBlockState(cell))) break;
            if (!isFrame(level, cell.below(), spec)) return Optional.empty();
            width++;
        }
        if (width < spec.minWidth() || width > spec.maxWidth()) return Optional.empty();
        if (!isFrame(level, bottomLeft.relative(right, width), spec)) return Optional.empty();

        // Height: rows of interior cells bounded by frame on both sides.
        int height = 0;
        rows:
        while (height <= spec.maxHeight()) {
            BlockPos rowStart = bottomLeft.above(height);
            for (int i = 0; i < width; i++) {
                if (!spec.isInterior(level.getBlockState(rowStart.relative(right, i)))) break rows;
            }
            if (!isFrame(level, rowStart.relative(left), spec) || !isFrame(level, rowStart.relative(right, width), spec)) {
                return Optional.empty();
            }
            height++;
        }
        if (height < spec.minHeight() || height > spec.maxHeight()) return Optional.empty();
        for (int i = 0; i < width; i++) {
            if (!isFrame(level, bottomLeft.relative(right, i).above(height), spec)) return Optional.empty();
        }

        List<BlockPos> anchors = new ArrayList<>();
        if (spec.anchor().isPresent()) {
            for (int i = 0; i < width; i++) collectAnchor(level, bottomLeft.relative(right, i).below(), spec, anchors);
            for (int i = 0; i < width; i++) collectAnchor(level, bottomLeft.relative(right, i).above(height), spec, anchors);
            for (int y = 0; y < height; y++) {
                collectAnchor(level, bottomLeft.above(y).relative(left), spec, anchors);
                collectAnchor(level, bottomLeft.above(y).relative(right, width), spec, anchors);
            }
            if (anchors.isEmpty()) return Optional.empty();
        }

        return Optional.of(new PortalFrame(axis, bottomLeft.immutable(), width, height, List.copyOf(anchors)));
    }

    /**
     * Finds the portal whose frame contains {@code anchor}. The anchor may sit in any side of the frame,
     * so the opening is searched for on every side of it.
     */
    public static Optional<PortalFrame> findAroundAnchor(LevelAccessor level, BlockPos anchor, Direction.Axis preferred, PortalSpec spec) {
        for (Direction side : new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            Optional<PortalFrame> frame = find(level, anchor.relative(side), preferred, spec);
            if (frame.isPresent() && frame.get().anchors().contains(anchor)) return frame;
        }
        return Optional.empty();
    }

    public Direction right() {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    public List<BlockPos> interior() {
        List<BlockPos> cells = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int i = 0; i < width; i++) cells.add(bottomLeft.relative(right(), i).above(y));
        }
        return cells;
    }

    public boolean isLit(LevelAccessor level, PortalSpec spec) {
        return interior().stream().allMatch(p -> level.getBlockState(p).is(spec.portalBlock()));
    }

    public void light(LevelAccessor level, PortalSpec spec) {
        BlockState portal = spec.portalBlock().defaultBlockState();
        if (portal.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            portal = portal.setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        }
        // Same flags as vanilla: send to clients, skip neighbour shape updates so the portal doesn't collapse mid-fill.
        for (BlockPos cell : interior()) level.setBlock(cell, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** Bottom-centre of the opening, where arriving entities are placed. */
    public Vec3 arrivalPoint() {
        Direction right = right();
        double half = width / 2.0;
        return new Vec3(
                bottomLeft.getX() + 0.5 + right.getStepX() * (half - 0.5),
                bottomLeft.getY(),
                bottomLeft.getZ() + 0.5 + right.getStepZ() * (half - 0.5));
    }

    private static boolean isFrame(LevelAccessor level, BlockPos pos, PortalSpec spec) {
        return spec.isFrame(level.getBlockState(pos), level, pos);
    }

    private static void collectAnchor(LevelAccessor level, BlockPos pos, PortalSpec spec, List<BlockPos> out) {
        if (spec.isAnchor(level.getBlockState(pos))) out.add(pos.immutable());
    }

    private static Direction.Axis other(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }
}
