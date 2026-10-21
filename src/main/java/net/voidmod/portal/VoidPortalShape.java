package net.voidmod.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.voidmod.VoidTags;
import net.voidmod.registry.VoidBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A rectangular frame of {@link VoidTags#VOID_PORTAL_FRAME} blocks around an opening, containing at least one
 * {@link VoidTags#VOID_PORTAL_ANCHOR} block. Corners are optional, as with Nether portals.
 *
 * @param bottomLeft lowest interior cell on the negative side of the axis (west for X, north for Z)
 * @param anchors    anchor-tagged frame blocks, ordered bottom row first, then top row, then sides
 */
public record VoidPortalShape(Direction.Axis axis, BlockPos bottomLeft, int width, int height, List<BlockPos> anchors) {
    public static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    /** Tries the preferred axis first, then the other, like vanilla's empty portal search. */
    public static Optional<VoidPortalShape> find(LevelAccessor level, BlockPos inside, Direction.Axis preferred) {
        Optional<VoidPortalShape> shape = findOnAxis(level, inside, preferred);
        return shape.isPresent() ? shape : findOnAxis(level, inside, other(preferred));
    }

    public static Optional<VoidPortalShape> findOnAxis(LevelAccessor level, BlockPos inside, Direction.Axis axis) {
        if (!isInterior(level.getBlockState(inside))) return Optional.empty();
        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction right = left.getOpposite();

        // Drop to the floor of the opening.
        BlockPos pos = inside;
        for (int i = 0; i < MAX_HEIGHT && isInterior(level.getBlockState(pos.below())); i++) pos = pos.below();
        if (!isFrame(level.getBlockState(pos.below()))) return Optional.empty();

        // Walk to the left edge along the floor.
        for (int i = 0; i < MAX_WIDTH && isInterior(level.getBlockState(pos.relative(left))); i++) pos = pos.relative(left);
        if (!isFrame(level.getBlockState(pos.relative(left)))) return Optional.empty();
        BlockPos bottomLeft = pos;

        // Width: every floor cell must sit on frame.
        int width = 0;
        while (width <= MAX_WIDTH) {
            BlockPos cell = bottomLeft.relative(right, width);
            if (!isInterior(level.getBlockState(cell))) break;
            if (!isFrame(level.getBlockState(cell.below()))) return Optional.empty();
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) return Optional.empty();
        if (!isFrame(level.getBlockState(bottomLeft.relative(right, width)))) return Optional.empty();

        // Height: rows of interior cells bounded by frame on both sides.
        int height = 0;
        rows:
        while (height <= MAX_HEIGHT) {
            BlockPos rowStart = bottomLeft.above(height);
            for (int i = 0; i < width; i++) {
                if (!isInterior(level.getBlockState(rowStart.relative(right, i)))) break rows;
            }
            if (!isFrame(level.getBlockState(rowStart.relative(left)))
                    || !isFrame(level.getBlockState(rowStart.relative(right, width)))) {
                return Optional.empty();
            }
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) return Optional.empty();
        for (int i = 0; i < width; i++) {
            if (!isFrame(level.getBlockState(bottomLeft.relative(right, i).above(height)))) return Optional.empty();
        }

        List<BlockPos> anchors = new ArrayList<>();
        for (int i = 0; i < width; i++) collectAnchor(level, bottomLeft.relative(right, i).below(), anchors);
        for (int i = 0; i < width; i++) collectAnchor(level, bottomLeft.relative(right, i).above(height), anchors);
        for (int y = 0; y < height; y++) {
            collectAnchor(level, bottomLeft.above(y).relative(left), anchors);
            collectAnchor(level, bottomLeft.above(y).relative(right, width), anchors);
        }
        if (anchors.isEmpty()) return Optional.empty();

        return Optional.of(new VoidPortalShape(axis, bottomLeft.immutable(), width, height, List.copyOf(anchors)));
    }

    /**
     * Finds the portal whose frame contains {@code anchor}. The anchor may sit in any side of the frame,
     * so the opening is searched for on every side of it.
     */
    public static Optional<VoidPortalShape> findAroundAnchor(LevelAccessor level, BlockPos anchor, Direction.Axis preferred) {
        for (Direction side : new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            Optional<VoidPortalShape> shape = find(level, anchor.relative(side), preferred);
            if (shape.isPresent() && shape.get().anchors().contains(anchor)) return shape;
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

    public boolean isLit(LevelAccessor level) {
        return interior().stream().allMatch(p -> level.getBlockState(p).is(VoidBlocks.VOID_PORTAL.get()));
    }

    public void light(LevelAccessor level) {
        BlockState portal = VoidBlocks.VOID_PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.AXIS, axis);
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

    static boolean isFrame(BlockState state) {
        return state.is(VoidTags.VOID_PORTAL_FRAME);
    }

    static boolean isInterior(BlockState state) {
        return state.isAir()
                || state.getBlock() instanceof BaseFireBlock
                || state.is(VoidBlocks.VOID_PORTAL.get())
                || state.is(VoidTags.VOID_PORTAL_REPLACEABLE);
    }

    private static void collectAnchor(LevelAccessor level, BlockPos pos, List<BlockPos> out) {
        if (level.getBlockState(pos).is(VoidTags.VOID_PORTAL_ANCHOR)) out.add(pos.immutable());
    }

    private static Direction.Axis other(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }
}
