package org.mcsg.survivalgames.commands;

import java.text.DecimalFormat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;
import org.mcsg.survivalgames.SettingsManager;
import org.mcsg.survivalgames.points.PlayerStats;
import org.mcsg.survivalgames.points.PointQueries;

public class Stats implements SubCommand {

    @Override
    public boolean onCommand(Player player, String[] args) {
        
        if (args.length == 1) {

            if (player.hasPermission(permission())) {
                displayStats(player, args[0]);
                return true;
            } else {
                MessageManager.getInstance().sendFMessage(PrefixType.WARNING, "error.nopermission", player);
            }
        } else {

            if (player.hasPermission(permission())) {
                displayStats(player, player.getName());
                return true;
            } else {
                MessageManager.getInstance().sendFMessage(PrefixType.WARNING, "error.nopermission", player);
            }
        }
        return true;
    }

    public void displayStats(Player requester, String player) {
        MessageManager msg = MessageManager.getInstance();
        
        PlayerStats ps = PointQueries.getStats(player.toLowerCase());
        
        if(ps == null){
            msg.sendMessage(PrefixType.INFO, "The name you have entered could not be found! Please try again", requester);
            return;
        }

        msg.sendMessage(PrefixType.INFO, ChatColor.GOLD + "" + ChatColor.BOLD + "" + player + "'s stats", requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Rank: " + ChatColor.YELLOW + ps.getRankPrefix(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Points: " + ChatColor.YELLOW + ps.getPoints(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Wins: " + ChatColor.YELLOW + ps.getWins(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Kills: " + ChatColor.YELLOW + ps.getKills(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Deaths: " + ChatColor.YELLOW + ps.getDeaths(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " K/D: " + ChatColor.YELLOW + String.format("%.2f", ps.getKDA()), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Games Played: " + ChatColor.YELLOW + ps.getGamesPlayed(), requester);
        msg.sendMessage(PrefixType.INFO, ChatColor.GREEN + " Time Played: " + ChatColor.YELLOW + ps.getFormattedPlaytime(), requester);
    }

    @Override
    public String help(Player p) {
        return "/sg stats - " + SettingsManager.getInstance().getMessageConfig().getString("messages.help.stats", "Shows you your own stats");
    }

    @Override
    public String permission() {
        return "sg.player.stats";
    }
}
