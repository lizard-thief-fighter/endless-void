package net.endlessvoid.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.endlessvoid.VoidMod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code endless_void:portal_type} datapack registry. It's synced to clients, because client-side block updates
 * also check frames. Definitions load with the world; the tags they reference also reload with /reload.
 */
public final class PortalTypes {
    public static final ResourceKey<Registry<PortalType>> REGISTRY = ResourceKey.createRegistryKey(VoidMod.id("portal_type"));
    public static final ResourceKey<PortalType> VOID = ResourceKey.create(REGISTRY, VoidMod.id("void"));
    public static final ResourceKey<PortalType> NETHER = ResourceKey.create(REGISTRY, VoidMod.id("nether"));

    private static final Set<ResourceKey<PortalType>> WARNED = ConcurrentHashMap.newKeySet();

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, PortalType.CODEC, PortalType.CODEC);
    }

    public static PortalType get(RegistryAccess access, ResourceKey<PortalType> key) {
        return access.registry(REGISTRY).flatMap(registry -> registry.getOptional(key)).orElseGet(() -> {
            if (WARNED.add(key)) VoidMod.LOGGER.warn("Portal type {} is missing; using built-in defaults", key.location());
            return key == NETHER ? PortalType.defaultNether() : PortalType.defaultVoid();
        });
    }

    public static PortalType get(LevelReader level, ResourceKey<PortalType> key) {
        return get(level.registryAccess(), key);
    }

    /**
     * Frame check for vanilla Nether portals. Obsidian counts only if the nether definition includes it; other
     * modded blocks that override NeoForge's isPortalFrame still work.
     */
    public static boolean isNetherFrame(BlockState state, BlockGetter getter, BlockPos pos) {
        if (getter instanceof LevelReader level && state.is(get(level, NETHER).frame())) return true;
        if (!(getter instanceof LevelReader)) return state.isPortalFrame(getter, pos);
        return !state.is(Blocks.OBSIDIAN) && state.isPortalFrame(getter, pos);
    }

    private static volatile boolean warnedGeneratedFrame;

    /**
     * A generated exit frame built from a block outside the frame set would break on the next block update
     * (vanilla re-checks Nether frames), so warn once instead of failing silently.
     */
    public static void warnIfNotFrame(PortalType type, Block generated) {
        if (!warnedGeneratedFrame && !type.frame().contains(generated.builtInRegistryHolder())) {
            warnedGeneratedFrame = true;
            VoidMod.LOGGER.warn("Nether portal type's generated_frame {} is not in its frame set; generated portals will break",
                    BuiltInRegistries.BLOCK.getKey(generated));
        }
    }

    private PortalTypes() {}
}
