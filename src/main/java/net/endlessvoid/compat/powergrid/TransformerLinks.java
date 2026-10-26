package net.endlessvoid.compat.powergrid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.endlessvoid.VoidMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where both halves of each Power Grid interdimensional transformer are. Power Grid finds a broken transformer's
 * other half by asking the portal for the matching spot across it, which can miss when the two portal openings
 * differ in size (an Ancient City frame against a Void return portal). Recording the halves when Power Grid links
 * them lets the other half always be found. Saved with the Overworld; only used by {@code PowerGridTransformerLinkMixin}.
 */
public final class TransformerLinks extends SavedData {
    private static final String ID = VoidMod.MOD_ID + "_transformer_links";

    /** {@code pos} is the linked (upper) half; the lower half is directly below it. */
    private record Half(UUID id, boolean secondary, GlobalPos pos) {
        static final Codec<Half> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Half::id),
                Codec.BOOL.fieldOf("secondary").forGetter(Half::secondary),
                GlobalPos.CODEC.fieldOf("pos").forGetter(Half::pos)
        ).apply(i, Half::new));
    }

    private final Map<UUID, GlobalPos[]> halves = new HashMap<>();

    private static TransformerLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TransformerLinks::new, TransformerLinks::load, null), ID);
    }

    /** Called when Power Grid links a half. */
    public static void record(ServerLevel level, UUID id, boolean secondary, BlockPos pos) {
        TransformerLinks data = get(level.getServer());
        data.halves.computeIfAbsent(id, $ -> new GlobalPos[2])[secondary ? 1 : 0] = GlobalPos.of(level.dimension(), pos);
        data.setDirty();
    }

    /**
     * Called after Power Grid has handled a destroyed half. If the other half still stands, it's removed the same way
     * Power Grid removes it (both blocks, no drops). The link is forgotten first, so that removal doesn't recurse.
     */
    public static void removeOtherHalf(ServerLevel level, UUID id, boolean secondary, BlockEntityType<?> type) {
        TransformerLinks data = get(level.getServer());
        GlobalPos[] pair = data.halves.remove(id);
        if (pair == null) return;
        data.setDirty();
        GlobalPos other = pair[secondary ? 0 : 1];
        if (other == null) return;
        ServerLevel otherLevel = level.getServer().getLevel(other.dimension());
        if (otherLevel == null || otherLevel.getBlockEntity(other.pos()) == null
                || otherLevel.getBlockEntity(other.pos()).getType() != type) return;
        Block block = otherLevel.getBlockState(other.pos()).getBlock();
        otherLevel.setBlock(other.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        BlockPos lower = other.pos().below();
        if (otherLevel.getBlockState(lower).is(block)) {
            otherLevel.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        List<Half> list = new ArrayList<>();
        halves.forEach((id, pair) -> {
            if (pair[0] != null) list.add(new Half(id, false, pair[0]));
            if (pair[1] != null) list.add(new Half(id, true, pair[1]));
        });
        Half.CODEC.listOf().encodeStart(NbtOps.INSTANCE, list)
                .resultOrPartial(VoidMod.LOGGER::error)
                .ifPresent(encoded -> tag.put("halves", encoded));
        return tag;
    }

    private static TransformerLinks load(CompoundTag tag, HolderLookup.Provider registries) {
        TransformerLinks data = new TransformerLinks();
        if (tag.contains("halves")) {
            Half.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get("halves"))
                    .resultOrPartial(VoidMod.LOGGER::error)
                    .ifPresent(list -> list.forEach(half ->
                            data.halves.computeIfAbsent(half.id(), $ -> new GlobalPos[2])[half.secondary() ? 1 : 0] = half.pos()));
        }
        return data;
    }
}
