package org.mcsg.survivalgames.events;

import de.tr7zw.itemnbtapi.NBTItem;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.Game.GameMode;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.SurvivalGames;
import org.mcsg.survivalgames.util.ChestRatioStorage;

public class ChestReplaceEvent implements Listener {

    private Random rand = new Random();

   
    /**
     * FIXES FURNACE/DISPENSER BUG - TO BE REPLACED WITH CONFIGURABLE LOOT IN
     * THE FUTURE
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void ContainerListener(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockState clicked = e.getClickedBlock().getState();
            if (clicked instanceof Furnace || clicked instanceof Dropper || clicked instanceof Hopper || clicked instanceof Dispenser) {
                int gameid = GameManager.getInstance().getPlayerGameId(e.getPlayer());
                if (gameid != -1) {
                    Game game = GameManager.getInstance().getGame(gameid);
                    if (game.getMode() == GameMode.INGAME) {
                        ((InventoryHolder) clicked).getInventory().clear();
                    }
                }
            }
        }
    }

    
    /**
     * For debugging purposes
     * 
     * @param e 
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void ChestListenerDebug(PlayerInteractEvent e) {

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.getPlayer().sendMessage("YOU CLICKED A CHEST. DOING SHIT HOMEBOI");
            BlockState clicked = e.getClickedBlock().getState();

            if (clicked instanceof Chest || clicked instanceof DoubleChest) {
             
                      
                            Chest chest = (Chest) clicked.getBlock().getState();
                            
                            Inventory[] chestInventory = new Inventory[]{((Chest) clicked).getBlockInventory()};
                            
                            ItemStack item = chestInventory[0].getItem(0);

                            int level = (item != null && item.getType() == Material.WOOL) ? item.getData().getData() + 1 : 1;

                            level = ChestRatioStorage.getInstance().getLevel(level);
                            SurvivalGames.debug(Arrays.toString(chestInventory) + " " + level);

                            for (Inventory chestContents : chestInventory) {

                                chestContents.setContents(new ItemStack[chestContents.getContents().length]);

                                //This here randomizes the placement of the items in the level given.
                                for (ItemStack i : ChestRatioStorage.getInstance().getItems(level)) {

                                    int l = rand.nextInt(26); // Why 26 you may ask? Muahahaha. Jk. This is because the slots in the chest are numbered from 0-26. 

                                    while (chestContents.getItem(l) != null) {
                                        l = rand.nextInt(26);
                                    }

                                    chestContents.setItem(l, i);
                                }

                            }
                        
                       
                    
                    }
                }
            }
          
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void ChestListener(PlayerInteractEvent e) {

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            
            BlockState clicked = e.getClickedBlock().getState();

            if (clicked instanceof Chest || clicked instanceof DoubleChest) {
                int gameid = GameManager.getInstance().getPlayerGameId(e.getPlayer());

                if (gameid != -1) {
                    Game game = GameManager.getInstance().getGame(gameid);

                    if (game.getMode() == GameMode.INGAME) {
                        HashSet<Block> openedChest = GameManager.openedChest.get(gameid);
                        openedChest = (openedChest == null) ? new HashSet<Block>() : openedChest;

                        if (!openedChest.contains(e.getClickedBlock())) {
                            
                            
                            Inventory[] chestInventory = ((clicked instanceof Chest)) ? new Inventory[]{((Chest) clicked).getBlockInventory()} : new Inventory[]{
                                ((DoubleChest) clicked).getLeftSide().getInventory(),
                                ((DoubleChest) clicked).getRightSide().getInventory()};

                            
                            ItemStack item = chestInventory[0].getItem(0);

                            int level = (item != null && item.getType() == Material.WOOL) ? item.getData().getData() + 1 : 1;

                            level = ChestRatioStorage.getInstance().getLevel(level);
                            SurvivalGames.debug(Arrays.toString(chestInventory) + " " + level);

                            for (Inventory chestContents : chestInventory) {

                                chestContents.setContents(new ItemStack[chestContents.getContents().length]);

                                //This here randomizes the placement of the items in the level given.
                                for (ItemStack i : ChestRatioStorage.getInstance().getItems(level)) {

                                    int l = rand.nextInt(26); // Why 26 you may ask? Muahahaha. Jk. This is because the slots in the chest are numbered from 0-26. 

                                    while (chestContents.getItem(l) != null) {
                                        l = rand.nextInt(26);
                                    }

                                    chestContents.setItem(l, i);
                                }

                            }
                        }
                        openedChest.add(e.getClickedBlock());
                        GameManager.openedChest.put(gameid, openedChest);
                    } else {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
