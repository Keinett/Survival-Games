package org.mcsg.survivalgames.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.mcsg.survivalgames.GameManager;

public class TeleportEvent implements Listener {

    /**
     * Modification 7/1/2016 Deny teleportation of users in spectator gamemode
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();

        int id = GameManager.getInstance().getPlayerGameId(p);

        
        if ((p.getGameMode() == org.bukkit.GameMode.SPECTATOR) && !p.hasMetadata("click_teleport")) {
            p.sendMessage(ChatColor.RED + "Teleportation using the spectator mode menu is not allowed!");
            event.setCancelled(true);
        }
        
        p.removeMetadata("click_teleport", GameManager.getInstance().getPlugin());

        if (id == -1) {
            return;
        }

        if (GameManager.getInstance().getGame(id).isPlayerActive(p) && event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            p.sendMessage(ChatColor.RED + " Cannot teleport while ingame!");
            event.setCancelled(true);
        }
        
        
    }

}
