package org.mcsg.survivalgames.commands;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;

public class Inventory implements SubCommand {

	@Override
	public boolean onCommand(Player player, String[] args) {
            try {
                if (!player.isOp()) {
                    MessageManager.getInstance().sendFMessage(MessageManager.PrefixType.ERROR, "error.nopermission", player);
                    return false;
                }
                
                Player p = player;
                
                YamlConfiguration c = new YamlConfiguration();
                c.set("inventory.content", p.getInventory().getContents());
                c.save(new File(GameManager.getInstance().getPlugin().getDataFolder(), p.getName()+".yml"));
                
                MessageManager.getInstance().sendMessage(PrefixType.INFO, "Inventory data stored: " + p.getName() + ".yml.", player);

                return true;
            } catch (IOException ex) {
                Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
	}

	@Override
	public String help(Player p) {
                // maybe not needed, keeping here for reference
		// return "/sg inventory - " + SettingsManager.getInstance().getMessageConfig().getString("messages.help.vote", "Votes to start the game");
                return null;
	}

	@Override
	public String permission() {
		return "sg.arena.inventory";
	}
}