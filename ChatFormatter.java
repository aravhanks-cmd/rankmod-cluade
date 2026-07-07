package com.yourserver.rankmod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Rewrites chat messages as "[RANK] Name: message", colored per rank.
 *
 * NOTE: Minecraft's chat pipeline (signed messages, ChatType decorations)
 * has shifted across versions. This implementation cancels the default
 * chat handling and rebroadcasts a plain formatted message itself, which
 * is the simplest approach that's stayed stable across 1.19+. If
 * ServerChatEvent's method names differ slightly in your exact 26.1 build,
 * check docs.neoforged.net's "Server Chat Event" page - getPlayer() and
 * getRawText() are usually the two you need.
 */
public class ChatFormatter {

    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        RankManager manager = RankManager.get(player.serverLevel().getServer().overworld());
        Rank rank = manager.getRank(player);

        Component formatted = Component.empty()
                .append(Component.literal("[" + rank.name() + "] ").withStyle(rank.color()))
                .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(event.getRawText()).withStyle(ChatFormatting.WHITE));

        event.setCanceled(true);
        player.serverLevel().getServer().getPlayerList().broadcastSystemMessage(formatted, false);
    }
}
