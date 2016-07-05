package org.mcsg.survivalgames.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mcsg.survivalgames.GameManager;

public class BandageUse implements Listener {

    @EventHandler
    
    /**
     * Merged from ThunderGemios10/sshipway commit 5db13658605d477beaa13b8c899cf0bca170a9d9
     * 
     * Fix bandages
     * Fix the bug that makes bandages not work
     * 
     * Modification 7/3/2016: Modification to work only in arenas crouton
     */
    public void onBandageUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        Boolean active = GameManager.getInstance().isPlayerActive(p);

        if (!active) {
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getPlayer().getItemInHand().getType() == Material.PAPER) {
                e.getPlayer().getInventory().removeItem(new ItemStack(Material.PAPER, 1));
                
                double newhealth = e.getPlayer().getHealth() + 10;

                if ((newhealth > 20.0) || (newhealth < 0)) {
                    newhealth = 20.0;
                }

                e.getPlayer().setHealth(newhealth);
                e.getPlayer().sendMessage(ChatColor.GREEN + "You used a bandage and got 5 hearts.");
            }
        }
    }
}
