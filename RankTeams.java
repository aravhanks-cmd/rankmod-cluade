package com.yourserver.rankmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Puts each player on a per-rank scoreboard team purely so the tab list
 * (and nametags) show a colored [RANK] prefix and group/sort correctly.
 *
 * Vanilla clients sort the tab list by team name alphabetically, so the
 * team names below are prefixed with a sort digit (0 = highest) to force
 * this order regardless of who joined first:
 * OWNER, CO-OWNER, ADMIN, MOD, BUILDER, OG, MVP, VIP, MEMBER.
 * The digit is never actually shown to players; only the prefix/color set
 * on the team is visible.
 */
public class RankTeams {

    private static String teamNameFor(Rank rank) {
        return switch (rank) {
            case OWNER -> "0_owner";
            case COOWNER -> "1_coowner";
            case ADMIN -> "2_admin";
            case MOD -> "3_mod";
            case BUILDER -> "4_builder";
            case OG -> "5_og";
            case MVP -> "6_mvp";
            case VIP -> "7_vip";
            case MEMBER -> "8_member";
        };
    }

    /** Moves the player onto the scoreboard team matching their current rank. */
    public static void assign(ServerPlayer player, Rank rank) {
        Scoreboard scoreboard = player.serverLevel().getScoreboard();
        String teamName = teamNameFor(rank);

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setPlayerPrefix(Component.literal("[" + rank.displayName() + "] ").withStyle(rank.color()));
            team.setColor(rank.color());
            // Keep normal PvP/visibility behavior - the team here exists only
            // for tab-list sorting and the prefix, not gameplay restrictions.
            team.setAllowFriendlyFire(true);
            team.setSeeFriendlyInvisibles(false);
            team.setNameTagVisibility(Team.Visibility.ALWAYS);
            team.setCollisionRule(Team.CollisionRule.ALWAYS);
        }

        // addPlayerToTeam removes the player from any other team automatically.
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }
}
