package com.yourserver.rankmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@Mod(RankMod.MOD_ID)
public class RankMod {

    public static final String MOD_ID = "rankmod";

    // Only this exact player is ever auto-granted / allowed to hold OWNER.
    private static final String OWNER_USERNAME = "AravOfAsh1";

    public RankMod(IEventBus modEventBus, ModContainer modContainer) {
        // No DisplayTest override here anymore - this mod is now required on
        // clients too (for the staff inventory-view screen), so we let
        // NeoForge's default behavior stand: any client missing this mod
        // gets disconnected automatically with a screen listing it as required.

        // Commands are registered on the game (NeoForge) event bus, not the mod bus.
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(CommandRestrictions::onCommand);
        NeoForge.EVENT_BUS.addListener(ChatFormatter::onChat);
        NeoForge.EVENT_BUS.addListener(OwnerProtection::onServerTick);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        RankCommands.register(event.getDispatcher());
        StaffViewCommands.register(event.getDispatcher());
        TempBanCommand.register(event.getDispatcher());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel overworld = player.serverLevel().getServer().overworld();
        RankManager manager = RankManager.get(overworld);

        if (player.getGameProfile().getName().equalsIgnoreCase(OWNER_USERNAME)
                && manager.getRank(player) != Rank.OWNER) {
            manager.setRank(player.getUUID(), Rank.OWNER);
        }

        RankTeams.assign(player, manager.getRank(player));
    }
}
