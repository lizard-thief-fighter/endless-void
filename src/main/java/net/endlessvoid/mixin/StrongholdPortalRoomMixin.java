package net.endlessvoid.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the vanilla stronghold portal room from placing any blocks: no End portal frames, lava, stairs or spawner.
 * The room is still laid out (stronghold generation retries until a layout contains one), so its space is reserved
 * but left as natural terrain, and the corridor leading to it ends at a dead end.
 * Jigsaw-based stronghold overhauls don't use this class and are unaffected.
 */
@Mixin(StrongholdPieces.PortalRoom.class)
public abstract class StrongholdPortalRoomMixin {
    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true)
    private void void$skipPortalRoom(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                     RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos,
                                     CallbackInfo ci) {
        ci.cancel();
    }
}
