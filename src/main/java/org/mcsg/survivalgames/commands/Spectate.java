package org.mcsg.survivalgames.commands;

import org.bukkit.entity.Player;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.SettingsManager;

public class Spectate implements SubCommand {

    @Override
    public boolean onCommand(Player player, String[] args) {
        if (!player.hasPermission(permission()) && !player.isOp()) {
            MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.nopermission", player);
            return true;
        }

        if (args.length == 0) {
            if (GameManager.getInstance().isSpectator(player)) {
                GameManager.getInstance().removeSpectator(player);
                return true;
            } else {
                MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.notspecified", player, "input-Game ID");
                return true;
            }
        }
        if (SettingsManager.getInstance().getSpawnCount(Integer.parseInt(args[0])) == 0) {
            MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.nospawns", player);
            return true;
        }
        if (GameManager.getInstance().isPlayerActive(player)) {
            MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.specingame", player);
            return true;
        }
        if (GameManager.getInstance().isSpectator(player)) {
            MessageManager.getInstance().sendMessage(MessageManager.PrefixType.ERROR, "You are already spectating a game! Type /spectate to leave.", player);
            return true;
        } else {

            try {
                Integer i = Integer.parseInt(args[0]);
                GameManager.getInstance().getGame(i).addSpectator(player);
            } catch (NumberFormatException ex) {
                MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.notanumber", player, "input-" + args[0]);
            }

            return true;
        }
    }

    @Override
    public String help(Player p) {
        return "/sg spectate <id> - " + SettingsManager.getInstance().getMessageConfig().getString("messages.help.spectate", "Spectate a running arena");
    }

    @Override
    public String permission() {
        return "sg.arena.spectate";
    }

}
