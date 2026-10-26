package net.endlessvoid.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Lights portals when fire appears inside a valid frame, for both the Void and the Nether. */
public final class PortalLighting {

    /** Returns true if the fire at {@code firePos} lit a portal (and so was replaced). */
    public static boolean tryLight(Level level, BlockPos firePos) {
        if (level.isClientSide()) return false;
        // The Void is tried first, so a frame that is valid for both becomes a Void portal.
        if (VoidPortals.isPortalDimension(level.dimension()) && light(level, firePos, PortalSpec.voidPortal(level))) {
            return true;
        }
        boolean netherDimension = level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
        return netherDimension && light(level, firePos, PortalSpec.netherPortal(level));
    }

    private static boolean light(Level level, BlockPos pos, PortalSpec spec) {
        Optional<PortalFrame> frame = PortalFrame.find(level, pos, Direction.Axis.X, spec);
        frame.ifPresent(f -> f.light(level, spec));
        return frame.isPresent();
    }

    private PortalLighting() {}
}
