package net.voidmod;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.voidmod.portal.PortalTypes;
import net.voidmod.registry.VoidBlockEntities;
import net.voidmod.registry.VoidBlocks;
import org.slf4j.Logger;

@Mod(VoidMod.MOD_ID)
public class VoidMod {
    public static final String MOD_ID = "void";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidMod(IEventBus modEventBus) {
        VoidBlocks.BLOCKS.register(modEventBus);
        VoidBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(PortalTypes::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
