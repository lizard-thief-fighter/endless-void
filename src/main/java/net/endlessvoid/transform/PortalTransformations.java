package net.endlessvoid.transform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.endlessvoid.VoidMod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Converts items when an entity changes dimension: thrown item entities, player inventories, entities that are
 * containers (chest/hopper minecarts, chest boats) and entities exposing NeoForge's item handler capability
 * (horses, donkeys, llamas, modded entities). Passengers are converted with their vehicle.
 */
@EventBusSubscriber(modid = VoidMod.MOD_ID)
public final class PortalTransformations {
    private static int processedTick = -1;
    private static final Set<UUID> processed = new HashSet<>();

    /** Runs last and only if no other listener cancelled the trip. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        PortalTransformer transformer = PortalTransformer.create(level, level.dimension(), event.getDimension());
        if (transformer == null) return;

        // An entity is converted once per trip, even if the event also fires for it as a passenger.
        int tick = level.getServer().getTickCount();
        if (tick != processedTick) {
            processedTick = tick;
            processed.clear();
        }
        transformOnce(entity, transformer);
        for (Entity passenger : entity.getIndirectPassengers()) transformOnce(passenger, transformer);
    }

    private static void transformOnce(Entity entity, PortalTransformer transformer) {
        if (processed.add(entity.getUUID())) transformEntity(entity, transformer);
    }

    /** Handles one kind of entity inventory; returns true if it handled the entity. */
    @FunctionalInterface
    public interface EntityHandler {
        boolean transform(Entity entity, PortalTransformer transformer);
    }

    private static final List<EntityHandler> HANDLERS = new CopyOnWriteArrayList<>();

    /** Lets integrations (e.g. Create contraptions) convert inventories this class doesn't know about. Checked first. */
    public static void registerEntityHandler(EntityHandler handler) {
        HANDLERS.add(handler);
    }

    public static void transformEntity(Entity entity, PortalTransformer transformer) {
        for (EntityHandler handler : HANDLERS) {
            if (handler.transform(entity, transformer)) return;
        }
        if (entity instanceof ItemEntity item) {
            transformItemEntity(item, transformer);
        } else if (entity instanceof Player player) {
            transformContainer(player.getInventory(), transformer, stack -> player.drop(stack, false));
        } else if (entity instanceof Container container) {
            transformContainer(container, transformer, entity::spawnAtLocation);
        } else {
            IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (handler != null) transformHandler(handler, transformer, entity::spawnAtLocation);
        }
    }

    /** Extra stacks become new item entities beside the original; they're still in the portal and follow it through. */
    public static void transformItemEntity(ItemEntity item, PortalTransformer transformer) {
        List<ItemStack> result = transformer.transform(item.getItem());
        if (result == null) return;
        item.setItem(result.getFirst());
        for (ItemStack extra : result.subList(1, result.size())) {
            ItemEntity copy = new ItemEntity(item.level(), item.getX(), item.getY(), item.getZ(), extra,
                    item.getDeltaMovement().x, item.getDeltaMovement().y, item.getDeltaMovement().z);
            item.level().addFreshEntity(copy);
        }
    }

    /** Overflow goes into free slots (or the player's inventory), and is dropped if there's no room. */
    public static void transformContainer(Container container, PortalTransformer transformer, Consumer<ItemStack> drop) {
        List<ItemStack> overflow = new ArrayList<>();
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            List<ItemStack> result = transformer.transform(container.getItem(slot));
            if (result == null) continue;
            container.setItem(slot, result.getFirst());
            overflow.addAll(result.subList(1, result.size()));
            changed = true;
        }
        for (ItemStack extra : overflow) {
            if (container instanceof Inventory inventory) {
                inventory.add(extra); // shrinks extra by whatever fits
            } else {
                insertIntoEmptySlot(container, extra);
            }
            if (!extra.isEmpty()) drop.accept(extra);
        }
        if (changed) container.setChanged();
    }

    public static void transformHandler(IItemHandler handler, PortalTransformer transformer, Consumer<ItemStack> drop) {
        List<ItemStack> overflow = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            List<ItemStack> converted = transformer.transform(stack);
            if (converted == null) continue;
            if (handler instanceof IItemHandlerModifiable modifiable) {
                List<ItemStack> result = converted;
                modifiable.setStackInSlot(slot, result.getFirst());
                overflow.addAll(result.subList(1, result.size()));
            } else {
                ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                if (extracted.isEmpty()) continue;
                List<ItemStack> result = transformer.transform(extracted);
                if (result == null) result = List.of(extracted);
                ItemStack leftover = handler.insertItem(slot, result.getFirst(), false);
                if (!leftover.isEmpty()) overflow.add(leftover);
                overflow.addAll(result.subList(1, result.size()));
            }
        }
        for (ItemStack extra : overflow) {
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, extra, false);
            if (!leftover.isEmpty()) drop.accept(leftover);
        }
    }

    private static void insertIntoEmptySlot(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, stack)) {
                container.setItem(slot, stack.copy());
                stack.setCount(0);
            }
        }
    }

    private PortalTransformations() {}
}
