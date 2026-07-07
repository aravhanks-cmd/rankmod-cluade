package com.yourserver.rankmod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.Set;

/**
 * Locks out vanilla (and any other mod's) commands for anyone below ADMIN.
 * Only the commands this mod adds (/rank, /tpa, /tpaccept) stay available
 * to everyone, regardless of rank.
 */
public class CommandRestrictions {

    private static final Set<String> ALWAYS_ALLOWED = Set.of("rank", "tpa", "tpaccept", "enderchest", "inventory", "tempban");

    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            // Console and command blocks are never restricted.
            return;
        }

        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) {
            input = input.substring(1);
        }
        String root = input.isEmpty() ? "" : input.split(" ", 2)[0];

        if (ALWAYS_ALLOWED.contains(root)) {
            return;
        }

        RankManager manager = RankManager.get(player.serverLevel().getServer().overworld());
        Rank rank = manager.getRank(player);

        if (!rank.atLeast(Rank.ADMIN)) {
            event.setCanceled(true);
            source.sendFailure(Component.literal(
                    "You need rank ADMIN or higher to use that command. Your rank: " + rank.name()));
        }
    }
}
