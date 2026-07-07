package com.yourserver.rankmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores each player's rank, persisted to the overworld's save data so it
 * survives server restarts. Attach/read this via {@link #get(ServerLevel)}.
 *
 * NOTE: NeoForge/Minecraft's SavedData codec plumbing has shifted a bit across
 * recent versions. If SavedDataType / the codec-based constructor below
 * doesn't compile against your exact 26.1 build, check the "Saved Data"
 * page on docs.neoforged.net for the current pattern - the surrounding
 * logic (the Map<UUID, Rank> and getRank/setRank methods) stays the same.
 */
public class RankManager extends SavedData {

    private static final String DATA_NAME = "rankmod_ranks";

    private static final Codec<Map<UUID, String>> RANK_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING);

    public static final SavedDataType<RankManager> TYPE = new SavedDataType<>(
            DATA_NAME,
            RankManager::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    RANK_MAP_CODEC.fieldOf("ranks").forGetter(m -> toStringMap(m.ranks))
            ).apply(instance, RankManager::fromStringMap))
    );

    private final Map<UUID, Rank> ranks = new HashMap<>();

    public RankManager() {
    }

    private static Map<UUID, String> toStringMap(Map<UUID, Rank> ranks) {
        Map<UUID, String> out = new HashMap<>();
        ranks.forEach((uuid, rank) -> out.put(uuid, rank.name()));
        return out;
    }

    private static RankManager fromStringMap(Map<UUID, String> stored) {
        RankManager manager = new RankManager();
        stored.forEach((uuid, name) -> {
            Rank rank = Rank.fromString(name);
            if (rank != null) {
                manager.ranks.put(uuid, rank);
            }
        });
        return manager;
    }

    /** Gets (or creates) the rank manager for a server level. Call with the overworld. */
    public static RankManager get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public Rank getRank(UUID playerId) {
        return ranks.getOrDefault(playerId, Rank.MEMBER);
    }

    public Rank getRank(Player player) {
        return getRank(player.getUUID());
    }

    public void setRank(UUID playerId, Rank rank) {
        ranks.put(playerId, rank);
        setDirty();
    }
}
