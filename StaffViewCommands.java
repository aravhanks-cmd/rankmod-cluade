package com.yourserver.rankmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

/**
 * /enderchest open <player> and /inventory open <player> - lets MOD/ADMIN/OWNER
 * open a live view of another player's ender chest or main inventory. Because
 * these are opened as plain vanilla chest-style menus wrapping the target's
 * actual Container object, edits the staff member makes apply instantly to
 * the target - it behaves exactly like opening your own chest.
 *
 * Note: /inventory open only shows the 36 main inventory + hotbar slots, not
 * armor or offhand. Including those alongside would need a custom menu type
 * with a matching client-side screen, which would require staff to have the
 * mod installed on their client too (breaking vanilla-client compatibility
 * for everyone else). If you want full armor/offhand visibility, let me know
 * and I can add a custom screen for staff to install.
 */
public class StaffViewCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enderchest")
                .then(Commands.literal("open")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(StaffViewCommands::openEnderChest))));

        dispatcher.register(Commands.literal("inventory")
                .then(Commands.literal("open")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(StaffViewCommands::openInventory))));
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer staff = requireStaff(ctx.getSource());
        if (staff == null) {
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        staff.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> ChestMenu.threeRows(id, playerInv, target.getEnderChestInventory()),
                Component.literal(target.getGameProfile().getName() + "'s Ender Chest")));

        return 1;
    }

    private static int openInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer staff = requireStaff(ctx.getSource());
        if (staff == null) {
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        staff.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> ChestMenu.fourRows(id, playerInv, target.getInventory()),
                Component.literal(target.getGameProfile().getName() + "'s Inventory")));

        return 1;
    }

    /** Returns the command-running player if they're MOD+, otherwise sends an error and returns null. */
    private static ServerPlayer requireStaff(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer staff = source.getPlayerOrException();
        ServerLevel overworld = staff.serverLevel().getServer().overworld();
        Rank rank = RankManager.get(overworld).getRank(staff);

        if (!rank.atLeast(Rank.MOD)) {
            source.sendFailure(Component.literal(
                    "You need rank MOD or higher to view another player's chest/inventory."));
            return null;
        }
        return staff;
    }
}
