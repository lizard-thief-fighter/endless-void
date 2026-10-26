package net.endlessvoid.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.portal.PortalForcer;
import net.endlessvoid.portal.PortalType;
import net.endlessvoid.portal.PortalTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Builds generated Nether exit frames (and their platforms) from the nether portal type's generated_frame. */
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;

    @ModifyExpressionValue(method = "createPortal", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;OBSIDIAN:Lnet/minecraft/world/level/block/Block;"))
    private Block void$generatedFrame(Block original) {
        PortalType type = PortalTypes.get(level, PortalTypes.NETHER);
        Block block = type.generatedFrame().orElse(original);
        PortalTypes.warnIfNotFrame(type, block);
        return block;
    }
}
