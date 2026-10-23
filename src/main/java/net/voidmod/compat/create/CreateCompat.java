package net.voidmod.compat.create;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.voidmod.VoidDimensions;
import net.voidmod.VoidMod;
import net.voidmod.portal.VoidPortalBlock;
import net.voidmod.registry.VoidBlocks;
import net.voidmod.transform.PortalTransformations;
import net.voidmod.transform.PortalTransformer;

/**
 * Create integration. Only loaded when Create is installed (see {@link VoidMod}).
 * <ul>
 *   <li>Train tracks link through Void portals like Nether portals. The exit comes from the portal's own
 *       destination logic, so a track follows the portal's stored link, and an unlinked Ancient City portal
 *       generates its Void return portal the first time a track is placed against it.</li>
 *   <li>Contraptions carried through portals by vanilla travel (e.g. minecart contraptions) convert their cargo.</li>
 *   <li>Train cargo converts once a carriage has fully crossed into another dimension
 *       ({@link CreateTrainTransformations}).</li>
 * </ul>
 */
public final class CreateCompat {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CreateCompat::commonSetup);
        NeoForge.EVENT_BUS.addListener(CreateTrainTransformations::onServerTick);
        PortalTransformations.registerEntityHandler(CreateCompat::transformContraption);
        VoidMod.LOGGER.info("Create detected; enabling Void portal tracks and cargo transformation");
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            VoidPortalBlock portal = VoidBlocks.VOID_PORTAL.get();
            PortalTrackProvider.REGISTRY.register(portal, (level, face) ->
                    PortalTrackProvider.fromPortal(level, face, Level.OVERWORLD, VoidDimensions.THE_VOID, portal));
        });
    }

    /**
     * Converts a contraption's mounted storage. Externally attached inventories (such as the chest minecart a
     * minecart contraption sits on) are left out: that entity converts its own inventory as a container.
     */
    private static boolean transformContraption(Entity entity, PortalTransformer transformer) {
        if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) return false;
        Contraption contraption = contraptionEntity.getContraption();
        if (contraption == null || contraption.getStorage() == null) return true;
        try {
            PortalTransformations.transformHandler(contraption.getStorage().getMountedItems(), transformer,
                    entity::spawnAtLocation);
        } catch (IllegalStateException uninitialized) {
            // Storage isn't set up yet (contraption still assembling); nothing to convert.
        }
        return true;
    }

    private CreateCompat() {}
}
