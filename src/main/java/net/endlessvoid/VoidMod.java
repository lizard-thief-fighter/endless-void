package net.endlessvoid;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.endlessvoid.portal.PortalTypes;
import net.endlessvoid.registry.VoidBlockEntities;
import net.endlessvoid.registry.VoidBlocks;
import net.endlessvoid.registry.VoidRecipes;
import org.slf4j.Logger;

@Mod(VoidMod.MOD_ID)
public class VoidMod {
    public static final String MOD_ID = "endless_void";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidMod(IEventBus modEventBus) {
        VoidBlocks.BLOCKS.register(modEventBus);
        VoidBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        VoidRecipes.RECIPE_TYPES.register(modEventBus);
        VoidRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(PortalTypes::register);
        // CreateCompat is only class-loaded when Create is present.
        if (ModList.get().isLoaded("create")) net.endlessvoid.compat.create.CreateCompat.init(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
