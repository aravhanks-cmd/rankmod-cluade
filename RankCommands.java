package com.yourserver.rankmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

public class RankCommands {

    // Only this exact player can ever hold the OWNER rank.
    private static final String OWNER_USERNAME = "AravOfAsh1";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        SuggestionProvider<CommandSourceStack> rankSuggestions = (context, builder) ->
                SharedSuggestionProvider.suggest(
                        Arrays.stream(Rank.values()).map(r -> r.name().toLowerCase()),
                        builder);

        // /rank set <player> <rank>   - op only (permission level 2)
        // /rank get <player>          - anyone can check
        dispatcher.register(Commands.literal("rank")
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests(rankSuggestions)
                                        .executes(RankCommands::runSetRank))))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(RankCommands::runGetRank))));

        // /tpa <player> - request to teleport to someone; open to everyone
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(RankCommands::runTpaRequest)));

        // /tpaccept - accept the most recent pending request to teleport to you
        dispatcher.register(Commands.literal("tpaccept")
                .executes(RankCommands::runTpaccept));
    }

    private static int runSetRank(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String rankName = StringArgumentType.getString(ctx, "rank");
        Rank rank = Rank.fromString(rankName);

        if (rank == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown rank '" + rankName + "'. Valid ranks: " + Arrays.toString(Rank.values())));
            return 0;
        }

        if (rank == Rank.OWNER || rank == Rank.COOWNER || rank == Rank.ADMIN || rank == Rank.MOD) {
            ServerPlayer sourcePlayer = ctx.getSource().getPlayer();
            boolean sourceIsTheOwner = sourcePlayer != null
                    && sourcePlayer.getGameProfile().getName().equalsIgnoreCase(OWNER_USERNAME);

            if (!sourceIsTheOwner) {
                ctx.getSource().sendFailure(Component.literal(
                        "Only " + OWNER_USERNAME + " can grant the " + rank.displayName() + " rank."));
                return 0;
            }
        }

        RankManager manager = RankManager.get(overworld(ctx.getSource()));
        manager.setRank(target.getUUID(), rank);
        RankTeams.assign(target, rank);

        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + "'s rank set to " + rank.name()), true);
        target.sendSystemMessage(Component.literal("Your rank is now " + rank.name() + "!"));
        return 1;
    }

    private static int runGetRank(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        RankManager manager = RankManager.get(overworld(ctx.getSource()));
        Rank rank = manager.getRank(target.getUUID());

        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + "'s rank is " + rank.name()), false);
        return 1;
    }

    private static int runTpaRequest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer requester;
        try {
            requester = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Only players can use /tpa."));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        if (target.getUUID().equals(requester.getUUID())) {
            source.sendFailure(Component.literal("You can't teleport to yourself."));
            return 0;
        }

        TpaManager.request(requester.getUUID(), target.getUUID());

        source.sendSuccess(() -> Component.literal(
                "Teleport request sent to " + target.getGameProfile().getName() + "."), false);
        target.sendSystemMessage(Component.literal(
                requester.getGameProfile().getName()
                        + " wants to teleport to you. Type /tpaccept to allow it."));
        return 1;
    }

    private static int runTpaccept(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target;
        try {
            target = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Only players can use /tpaccept."));
            return 0;
        }

        UUID requesterId = TpaManager.findRequesterFor(target.getUUID());
        if (requesterId == null) {
            source.sendFailure(Component.literal("You have no pending teleport requests."));
            return 0;
        }

        ServerLevel level = overworld(source);
        ServerPlayer requester = level.getServer().getPlayerList().getPlayer(requesterId);
        TpaManager.clear(requesterId);

        if (requester == null) {
            source.sendFailure(Component.literal("That player is no longer online."));
            return 0;
        }

        requester.teleportTo((ServerLevel) target.level(),
                target.getX(), target.getY(), target.getZ(),
                requester.getYRot(), requester.getXRot());

        source.sendSuccess(() -> Component.literal("Teleported " + requester.getGameProfile().getName() + " to you."), false);
        requester.sendSystemMessage(Component.literal("Teleported to " + target.getGameProfile().getName() + "."));
        return 1;
    }

    private static ServerLevel overworld(CommandSourceStack source) {
        return source.getServer().overworld();
    }
}
