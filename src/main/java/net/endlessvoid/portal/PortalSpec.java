package net.endlessvoid.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.endlessvoid.registry.VoidBlocks;

import java.util.Optional;

/** A portal type resolved against a level, plus the portal block it fills with. */
public record PortalSpec(Block portalBlock, BlockBehaviour.StatePredicate frame, HolderSet<Block> replaceable,
                         Optional<HolderSet<Block>> anchor, int minWidth, int maxWidth, int minHeight, int maxHeight) {

    /** Vanilla's Nether portal block re-checks its frame with these limits, so Nether sizes can't exceed them. */
    private static final int VANILLA_MIN_WIDTH = 2, VANILLA_MAX_WIDTH = 21, VANILLA_MIN_HEIGHT = 3, VANILLA_MAX_HEIGHT = 21;

    public static PortalSpec voidPortal(LevelReader level) {
        PortalType type = PortalTypes.get(level, PortalTypes.VOID);
        HolderSet<Block> frame = type.frame();
        // Links are keyed by anchor positions, so the Void portal always needs an anchor set.
        HolderSet<Block> anchor = type.anchor().orElseGet(() -> PortalType.defaultVoid().anchor().orElseThrow());
        return new PortalSpec(VoidBlocks.VOID_PORTAL.get(), (state, getter, pos) -> state.is(frame),
                type.replaceable(), Optional.of(anchor),
                type.minWidth(), type.maxWidth(), type.minHeight(), type.maxHeight());
    }

    public static PortalSpec netherPortal(LevelReader level) {
        PortalType type = PortalTypes.get(level, PortalTypes.NETHER);
        return new PortalSpec(Blocks.NETHER_PORTAL, PortalTypes::isNetherFrame, type.replaceable(), type.anchor(),
                Mth.clamp(type.minWidth(), VANILLA_MIN_WIDTH, VANILLA_MAX_WIDTH),
                Mth.clamp(type.maxWidth(), VANILLA_MIN_WIDTH, VANILLA_MAX_WIDTH),
                Mth.clamp(type.minHeight(), VANILLA_MIN_HEIGHT, VANILLA_MAX_HEIGHT),
                Mth.clamp(type.maxHeight(), VANILLA_MIN_HEIGHT, VANILLA_MAX_HEIGHT));
    }

    public boolean isFrame(BlockState state, BlockGetter level, BlockPos pos) {
        return frame.test(state, level, pos);
    }

    public boolean isInterior(BlockState state) {
        return state.isAir()
                || state.getBlock() instanceof BaseFireBlock
                || state.is(portalBlock)
                || state.is(replaceable);
    }

    public boolean isAnchor(BlockState state) {
        return anchor.isPresent() && state.is(anchor.get());
    }
}
