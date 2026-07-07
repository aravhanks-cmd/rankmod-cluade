package com.yourserver.rankmod;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Date;
import java.util.Optional;

/**
 * Safety net for the OWNER rank: if anyone bans or tempbans AravOfAsh1 -
 * whether through vanilla /ban, this mod's /tempban, or anything else that
 * writes to the ban list - this reverts it and instead temp-bans whoever
 * caused it for 3 days.
 *
 * This works by periodically checking the ban list (about once a second)
 * rather than intercepting specific commands. That's because /ban is a
 * vanilla command we don't want to reimplement just to intercept it, and a
 * banned owner can't log in to trigger a login-based check in the first
 * place. TempBanCommand also has its own immediate check for its own
 * command specifically, so this tick check is really just the backstop
 * for vanilla /ban or anything else.
 */
public class OwnerProtection {

    static final String OWNER_USERNAME = "AravOfAsh1";
    static final long RETALIATION_MILLIS = 3L * 24 * 60 * 60 * 1000; // 3 days

    private static int tickCounter = 0;

    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 20) { // roughly once a second
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        GameProfileCache cache = server.getProfileCache();
        if (cache == null) {
            return;
        }

        Optional<GameProfile> ownerProfile = cache.get(OWNER_USERNAME);
        if (ownerProfile.isEmpty()) {
            return;
        }

        UserBanList banList = server.getPlayerList().getBans();
        GameProfile profile = ownerProfile.get();
        if (!banList.isBanned(profile)) {
            return;
        }

        UserBanListEntry entry = banList.get(profile);
        String culpritName = entry != null ? entry.getSource() : null;

        banList.remove(profile);
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "[RankMod] Someone tried to ban the OWNER - the ban was reverted."), false);

        if (culpritName != null) {
            cache.get(culpritName).ifPresent(culprit -> retaliate(server, culprit));
        }
    }

    /** Reverts any existing ban/tempban on `culprit` and temp-bans them for 3 days instead. */
    static void retaliate(MinecraftServer server, GameProfile culprit) {
        UserBanList banList = server.getPlayerList().getBans();

        if (banList.isBanned(culprit)) {
            banList.remove(culprit);
        }

        Date expires = new Date(System.currentTimeMillis() + RETALIATION_MILLIS);
        banList.add(new UserBanListEntry(culprit, new Date(), "RankMod (auto)", expires,
                "Attempted to ban the server owner"));

        ServerPlayer online = server.getPlayerList().getPlayer(culprit.getId());
        if (online != null) {
            online.connection.disconnect(Component.literal(
                    "You have been temp-banned for 3 days: attempted to ban the server owner."));
        }
    }
}
