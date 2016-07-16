package org.mcsg.survivalgames.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.mcsg.survivalgames.SettingsManager;
import org.mcsg.survivalgames.points.PlayerStats;
import org.mcsg.survivalgames.points.PointQueries;

/**
 *
 * @author Administrator
 */
public class PlayerEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();

        PlayerStats ps = PointQueries.getStats(p.getName().toLowerCase());

        String prefix = ps.getRankPrefix();

        if (!SettingsManager.getInstance().getConfig().getBoolean("handlechat")) {

            String format = e.getFormat();

            if (prefix == null || prefix.isEmpty()) {

                e.setFormat(format.replace("{SURVIVALGAMES}", ""));

            } else {

                e.setFormat(format.replace("{SURVIVALGAMES}", prefix));
            }
            
        } else {

            e.setFormat(prefix + "" + p.getDisplayName() + ChatColor.RESET + ": " + e.getMessage());

        }
    }
}
