package com.yourserver.rankmod;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /tempban <player> <duration> [reason]
 *
 * Works exactly like vanilla /ban - same ban list, same login-block - except
 * an expiration date is attached, so the player is let back in automatically
 * once it passes. Vanilla's own login check already honors ban expiration,
 * so there's no extra "auto unban" logic needed on this end.
 *
 * Duration format: a compact string combining any of these units in any
 * combination, e.g. "1d12h", "2w", "3mo", "45m", "10s":
 *   y = years, mo = months, w = weeks, d = days, h = hours, m = minutes, s = seconds
 * (years/months are approximated as 365/30 days.)
 *
 * Restricted to ADMIN rank or higher (console/command blocks are always allowed).
 */
public class TempBanCommand {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)(mo|y|w|d|h|m|s)");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> run(ctx, null))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "reason")))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String reason) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        if (!isAuthorized(source)) {
            source.sendFailure(Component.literal("You need rank MOD or higher to use /tempban."));
            return 0;
        }

        String durationText = StringArgumentType.getString(ctx, "duration");
        long millis = parseDuration(durationText);
        if (millis <= 0) {
            source.sendFailure(Component.literal(
                    "Invalid duration '" + durationText + "'. Example formats: 1d12h, 2w, 45m, 3mo"));
            return 0;
        }

        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        MinecraftServer server = source.getServer();
        UserBanList banList = server.getPlayerList().getBans();

        String banReason = (reason == null || reason.isBlank()) ? "Banned by an operator" : reason;
        Date expires = new Date(System.currentTimeMillis() + millis);
        String sourceName = source.getTextName();

        int count = 0;
        for (GameProfile profile : profiles) {
            if (profile.getName().equalsIgnoreCase(OwnerProtection.OWNER_USERNAME)) {
                source.sendFailure(Component.literal(
                        "You can't ban the OWNER. You've been temp-banned for 3 days instead."));
                if (source.getEntity() instanceof ServerPlayer offender) {
                    OwnerProtection.retaliate(server, offender.getGameProfile());
                }
                continue;
            }

            if (banList.isBanned(profile)) {
                banList.remove(profile);
            }
            banList.add(new UserBanListEntry(profile, new Date(), sourceName, expires, banReason));
            count++;

            ServerPlayer online = server.getPlayerList().getPlayer(profile.getId());
            if (online != null) {
                online.connection.disconnect(Component.literal(
                        "You have been temporarily banned.\nReason: " + banReason
                                + "\nExpires: " + expires));
            }
        }

        int finalCount = count;
        source.sendSuccess(() -> Component.literal(
                "Temp-banned " + finalCount + " player(s) for " + durationText
                        + (reason != null ? " (" + reason + ")" : "")), true);
        return count;
    }

    private static boolean isAuthorized(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            Rank rank = RankManager.get(player.serverLevel().getServer().overworld()).getRank(player);
            return rank.atLeast(Rank.ADMIN);
        }
        // Console / command blocks are always trusted.
        return true;
    }

    private static long parseDuration(String text) {
        Matcher matcher = DURATION_PART.matcher(text.toLowerCase());
        long totalMillis = 0;
        boolean matchedAny = false;

        while (matcher.find()) {
            matchedAny = true;
            long amount = Long.parseLong(matcher.group(1));
            totalMillis += amount * unitMillis(matcher.group(2));
        }

        return matchedAny ? totalMillis : -1;
    }

    private static long unitMillis(String unit) {
        return switch (unit) {
            case "y" -> 365L * 24 * 60 * 60 * 1000;
            case "mo" -> 30L * 24 * 60 * 60 * 1000;
            case "w" -> 7L * 24 * 60 * 60 * 1000;
            case "d" -> 24L * 60 * 60 * 1000;
            case "h" -> 60L * 60 * 1000;
            case "m" -> 60L * 1000;
            case "s" -> 1000L;
            default -> 0L;
        };
    }
}
