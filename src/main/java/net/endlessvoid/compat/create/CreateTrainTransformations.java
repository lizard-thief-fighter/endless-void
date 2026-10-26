package net.endlessvoid.compat.create;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.endlessvoid.VoidMod;
import net.endlessvoid.transform.PortalTransformations;
import net.endlessvoid.transform.PortalTransformer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Trains don't change dimension through vanilla entity travel: Create keeps one carriage entity per dimension
 * while a carriage straddles a portal. So each carriage's dimension is tracked, and its cargo converts once both
 * bogeys are in the new dimension. A train that backs out before fully crossing doesn't convert.
 */
final class CreateTrainTransformations {
    /** Last dimension each carriage was fully inside. Weak keys, so removed trains drop out. */
    private static final Map<Carriage, ResourceKey<Level>> SETTLED = new WeakHashMap<>();

    static void onServerTick(ServerTickEvent.Post event) {
        Map<UUID, Train> trains = Create.RAILWAYS.trains;
        if (trains == null) return;
        MinecraftServer server = event.getServer();
        for (Train train : trains.values()) {
            if (train.carriages == null) continue;
            for (Carriage carriage : train.carriages) {
                ResourceKey<Level> dimension = settledDimension(carriage);
                if (dimension == null) continue;
                ResourceKey<Level> previous = SETTLED.put(carriage, dimension);
                if (previous != null && !previous.equals(dimension)) convertCargo(server, carriage, previous, dimension);
            }
        }
    }

    /** The dimension both bogeys are in, or null while the carriage is crossing (or has no track position). */
    private static @Nullable ResourceKey<Level> settledDimension(Carriage carriage) {
        ResourceKey<Level> leading = carriage.leadingBogey().getDimension();
        ResourceKey<Level> trailing = carriage.trailingBogey().getDimension();
        return leading != null && leading.equals(trailing) ? leading : null;
    }

    private static void convertCargo(MinecraftServer server, Carriage carriage, ResourceKey<Level> from, ResourceKey<Level> to) {
        ServerLevel level = server.getLevel(to);
        TrainCargoManager cargo = carriage.storage;
        if (level == null || cargo == null) return;
        PortalTransformer transformer = PortalTransformer.create(level, from, to);
        if (transformer == null) return;
        try {
            PortalTransformations.transformHandler(cargo.getMountedItems(), transformer, stack -> drop(carriage, stack));
        } catch (IllegalStateException uninitialized) {
            // Cargo storage isn't set up yet; nothing to convert.
        }
    }

    /** Overflow that no cargo slot can take is dropped at the carriage; it's lost if no carriage entity is loaded. */
    private static void drop(Carriage carriage, ItemStack stack) {
        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        if (entity != null) {
            entity.spawnAtLocation(stack);
        } else {
            VoidMod.LOGGER.warn("Dropped {} converted on a train with no loaded carriage", stack);
        }
    }

    private CreateTrainTransformations() {}
}
