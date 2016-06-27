package org.mcsg.survivalgames.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;

public class CompassEvents implements Listener {

    private final MessageManager msgmgr = MessageManager.getInstance();

    @EventHandler
    public void onCompassActivate(PlayerInteractEvent event) {
        if ((event.getPlayer().getInventory().getItemInHand().getType() == Material.COMPASS) && ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.LEFT_CLICK_AIR)))) {
            Player player = event.getPlayer();
            GameManager gm = GameManager.getInstance();

            int gameID = gm.getPlayerGameId(player);

            if (gameID != -1 || event.getPlayer().isOp()) {

                Player p = getClosestPlayer(getNearbyPlayers(event.getPlayer()), event.getPlayer());

                if (player.hasMetadata("compass_lastuse")) {
                    if ((System.currentTimeMillis() / 1000) - player.getMetadata("compass_lastuse").get(0).asLong() < 5) {
                        if (player.hasMetadata("compass_tracking")) {
                            msgmgr.sendMessage(PrefixType.INFO, "Tracking last known location of" + ChatColor.AQUA + " " + player.getMetadata("compass_tracking").get(0).asString(), player);
                        } else {
                            msgmgr.sendMessage(PrefixType.INFO, ChatColor.AQUA + "No players within a 200 block radius!", player);
                        }
                    } else {
                        player.removeMetadata("compass_lastuse", GameManager.getInstance().getPlugin());
                        player.removeMetadata("compass_tracking", GameManager.getInstance().getPlugin());
                    }
                } else {
                    if (p != null && !GameManager.getInstance().isSpectator(p)) {
                        event.getPlayer().setCompassTarget(p.getLocation());
                        msgmgr.sendMessage(PrefixType.INFO, "Tracking last known location of" + ChatColor.GREEN + " " + p.getName(), player);
                        player.setMetadata("compass_lastuse", new FixedMetadataValue(GameManager.getInstance().getPlugin(), System.currentTimeMillis() / 1000));
                        player.setMetadata("compass_tracking", new FixedMetadataValue(GameManager.getInstance().getPlugin(), p.getName()));

                    } else {
                        msgmgr.sendMessage(PrefixType.INFO, "No players within a 200 block radius!", player);
                        player.setMetadata("compass_lastuse", new FixedMetadataValue(GameManager.getInstance().getPlugin(), System.currentTimeMillis() / 1000));
                    }
                }
            } else {
                msgmgr.sendMessage(PrefixType.INFO, "The compass can only be used whilst in-game!", player);
            }
        }
    }

    public Player getClosestPlayer(ArrayList<Entity> nearbyPlayers, Player player) {
        if (nearbyPlayers == null) {
            return null;
        }
        Random r = new Random();
        Player target = null;

        HashMap<Entity, Double> distances = new HashMap();

        for (Entity nearPlayer : nearbyPlayers) {

            Location locPlayer = player.getLocation();

            Double distanceToTarget = locPlayer.distance(nearPlayer.getLocation());

            distances.put(nearPlayer, distanceToTarget);
        }

        Entry<Entity, Double> minimum = null;

        for (Entry<Entity, Double> entrySet : distances.entrySet()) {
            if (minimum == null || minimum.getValue() > entrySet.getValue()) {
                minimum = entrySet;
            }
        }

        target = (Player) minimum.getKey();

        return target;
    }

    public ArrayList<Entity> getNearbyPlayers(Player p) {

        Double radius = 200D;

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
