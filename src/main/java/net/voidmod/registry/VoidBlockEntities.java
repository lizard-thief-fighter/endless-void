package net.voidmod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.voidmod.VoidMod;
import net.voidmod.portal.VoidPortalBlockEntity;

public final class VoidBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoidMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // null datafixer type is standard for modded block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidPortalBlockEntity>> VOID_PORTAL =
            BLOCK_ENTITY_TYPES.register("void_portal",
                    () -> BlockEntityType.Builder.of(VoidPortalBlockEntity::new, VoidBlocks.VOID_PORTAL.get()).build(null));

    private VoidBlockEntities() {}
}
