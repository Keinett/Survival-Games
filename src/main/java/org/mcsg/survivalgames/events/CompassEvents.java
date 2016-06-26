package org.mcsg.survivalgames.events;

import java.util.ArrayList;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mcsg.survivalgames.CompassHandler;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;
import org.mcsg.survivalgames.util.CompassListener;

public class CompassEvents implements Listener {

    private final MessageManager msgmgr = MessageManager.getInstance();

    @EventHandler
    public void onCompassActivate(PlayerInteractEvent event) {
        if ((event.getPlayer().getInventory().getItemInHand().getType() == Material.COMPASS) && ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.LEFT_CLICK_AIR)))) {
            Player player = event.getPlayer();
            GameManager gm = GameManager.getInstance();

            int gameID = gm.getPlayerGameId(player);

            if (gameID != -1 || event.getPlayer().isOp()) {

                if (CompassHandler.getInstance().containsTracker(player)) {

                    CompassListener cl = CompassHandler.getInstance().getCompassListener(player);
                    cl.disable();

                } else {

                    activateCompass(player, gameID);

                }

            } else {
                msgmgr.sendMessage(PrefixType.INFO, "The compass can only be used whilst in-game!", player);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        if (((event.getEntity() instanceof Player)) && (CompassHandler.getInstance().containsTracker((Player) event.getEntity()))) {
            Player p = (Player) event.getEntity();
            CompassHandler.getInstance().stopPlayerCompassListener(p);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        if (CompassHandler.getInstance().containsTracker(event.getPlayer())) {
            Player p = event.getPlayer();
            CompassHandler.getInstance().stopPlayerCompassListener(p);
        }
    }

    private void activateCompass(Player player, Integer gid) {
        ArrayList nearbyPlayers = getNearbyPlayers(player, gid);
        Player targetP = getRandomPlayer(nearbyPlayers);
        if (targetP != null) {
            msgmgr.sendMessage(PrefixType.INFO, "Nearby player found! Tracking: " + ChatColor.BOLD + ChatColor.DARK_GREEN + targetP.getName(), player);
            CompassListener locationUpdater = new CompassListener(GameManager.getInstance().getGame(gid), player, targetP);
            locationUpdater.setTaskID(Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(GameManager.getInstance().getPlugin(), locationUpdater, 0L, 20));

        } else {
            msgmgr.sendMessage(PrefixType.INFO, "No nearby players found!", player);
        }
    }

//    public Player getRandomPlayer(ArrayList<Entity> nearbyPlayers) {
//
//        if (nearbyPlayers == null) {
//            return null;
//        }
//        
//        Random r = new Random();
//        Player target = null;
//        
//        do {
//            int max = nearbyPlayers.size();
//            int random = r.nextInt(max);
//            target = (Player) nearbyPlayers.get(random);
//            nearbyPlayers.remove(random);
//        } while ((!nearbyPlayers.isEmpty()) && (GameManager.getInstance().isSpectator(target)));
//        
//        
//        if ((target != null) && (GameManager.getInstance().isSpectator(target))) {
//            return null;
//        }
//        
//        return target;
//    }

    
    public Player getRandomPlayer(ArrayList<Entity> nearbyPlayers) {

        if (nearbyPlayers == null) {
            return null;
        }
        
        Random r = new Random();
        Player target = null;
        
        do {
            int max = nearbyPlayers.size();
            int random = r.nextInt(max);
            target = (Player) nearbyPlayers.get(random);
            nearbyPlayers.remove(random);
        } while ((!nearbyPlayers.isEmpty()));
        
        
        if ((target != null)) {
            return null;
        }
        
        return target;
    }
    
    
    public ArrayList<Entity> getNearbyPlayers(Player p, Integer gid) {

        Double radius = CompassHandler.getInstance().getCompassRadius();

        ArrayList nearbyEntities;
        nearbyEntities = (ArrayList) p.getNearbyEntities(radius, radius, radius);
        for (int i = 0; i < nearbyEntities.size(); i++) {
            Entity e = (Entity) nearbyEntities.get(i);
            if (!(e instanceof Player)) {
                nearbyEntities.remove(i);
                i--;
            }
        }
        if (nearbyEntities.isEmpty()) {
            return null;
        }
        return nearbyEntities;
    }
}
