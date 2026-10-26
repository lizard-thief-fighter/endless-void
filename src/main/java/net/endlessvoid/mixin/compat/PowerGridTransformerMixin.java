package net.endlessvoid.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.endlessvoid.registry.VoidBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create: Power Grid's interdimensional ("Nether") transformer finds its partner through Create's portal track
 * registry, which already includes the Void portal, but three checks hard-code the Nether portal block: forming the
 * transformer from cores, its shape check, and its load-time check. This makes those checks accept the Void portal.
 * <p>
 * {@code @Pseudo} with string targets: no build dependency, and nothing applies when Power Grid isn't installed.
 * {@code require = 0} so a Power Grid update that renames these methods disables the integration instead of
 * crashing the game.
 */
@Pseudo
@Mixin(targets = {
        "org.patryk3211.powergrid.electricity.transformer.TransformerCoreBlock",
        "org.patryk3211.powergrid.electricity.transformer.NetherTransformerBlock",
        "org.patryk3211.powergrid.electricity.transformer.NetherTransformerBlockEntity"
})
public abstract class PowerGridTransformerMixin {
    @WrapOperation(method = {"locate2x2", "updateShape", "initialize"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean void$acceptVoidPortal(BlockState state, Block block, Operation<Boolean> original) {
        return original.call(state, block) || (block == Blocks.NETHER_PORTAL && state.is(VoidBlocks.VOID_PORTAL.get()));
    }
}
