package net.voidmod.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.voidmod.VoidMod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stored links between portals, saved with the Overworld. Each anchor block of a portal maps to one anchor of the
 * portal it leads to. Anchors can't be mined or moved, so a link survives the frame being broken and resized.
 */
public class VoidPortalLinks extends SavedData {
    private static final String ID = "void_portal_links";

    private record Link(GlobalPos from, GlobalPos to) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("from").forGetter(Link::from),
                GlobalPos.CODEC.fieldOf("to").forGetter(Link::to)
        ).apply(i, Link::new));
    }

    private final Map<GlobalPos, GlobalPos> links = new HashMap<>();

    public static VoidPortalLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(VoidPortalLinks::new, VoidPortalLinks::load, null), ID);
    }

    /** The target anchor linked to any of these anchors, or null if the portal is unlinked. */
    public @Nullable GlobalPos find(ResourceKey<Level> dimension, List<BlockPos> anchors) {
        for (BlockPos anchor : anchors) {
            GlobalPos target = links.get(GlobalPos.of(dimension, anchor));
            if (target != null) return target;
        }
        return null;
    }

    /** Links both portals to each other. */
    public void link(ResourceKey<Level> fromDim, PortalFrame from, ResourceKey<Level> toDim, PortalFrame to) {
        GlobalPos fromAnchor = GlobalPos.of(fromDim, from.anchors().getFirst());
        GlobalPos toAnchor = GlobalPos.of(toDim, to.anchors().getFirst());
        for (BlockPos anchor : from.anchors()) links.put(GlobalPos.of(fromDim, anchor), toAnchor);
        for (BlockPos anchor : to.anchors()) links.put(GlobalPos.of(toDim, anchor), fromAnchor);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        List<Link> list = new ArrayList<>(links.size());
        links.forEach((from, to) -> list.add(new Link(from, to)));
        Link.CODEC.listOf().encodeStart(NbtOps.INSTANCE, list)
                .resultOrPartial(VoidMod.LOGGER::error)
                .ifPresent(encoded -> tag.put("links", encoded));
        return tag;
    }

    private static VoidPortalLinks load(CompoundTag tag, HolderLookup.Provider registries) {
        VoidPortalLinks data = new VoidPortalLinks();
        if (tag.contains("links")) {
            Link.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get("links"))
                    .resultOrPartial(VoidMod.LOGGER::error)
                    .ifPresent(list -> list.forEach(link -> data.links.put(link.from(), link.to())));
        }
        return data;
    }
}
