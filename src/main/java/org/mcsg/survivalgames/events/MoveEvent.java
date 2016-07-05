package org.mcsg.survivalgames.events;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.Game.GameMode;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.MessageManager;

public class MoveEvent implements Listener {

    HashMap<Player, Vector> playerpos = new HashMap<Player, Vector>();

    /**
     * Freezes players in place before game starts or during deathmatch
     * countdown
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void frozenSpawnHandler(PlayerMoveEvent e) {
        if (GameManager.getInstance().getPlayerGameId(e.getPlayer()) == -1) {
            playerpos.remove(e.getPlayer());
            return;
        }
        
        Game game = GameManager.getInstance().getGame(GameManager.getInstance().getPlayerGameId(e.getPlayer()));

        if (!game.inDeathmatchCountdown()) {
            if (game.getMode() == Game.GameMode.INGAME) {
                return;
            }
        }

        GameMode mode = GameManager.getInstance().getGameMode(GameManager.getInstance().getPlayerGameId(e.getPlayer()));
        
        if ((GameManager.getInstance().isPlayerActive(e.getPlayer()) && mode != Game.GameMode.INGAME) || game.inDeathmatchCountdown()) {
            if (playerpos.get(e.getPlayer()) == null) {
                playerpos.put(e.getPlayer(), e.getPlayer().getLocation().toVector());
                return;
            }
            Location l = e.getPlayer().getLocation();
            Vector v = playerpos.get(e.getPlayer());
            if (l.getBlockX() != v.getBlockX() || l.getBlockZ() != v.getBlockZ()) {
                l.setX(v.getBlockX() + .5);
                l.setZ(v.getBlockZ() + .5);
                l.setYaw(e.getPlayer().getLocation().getYaw());
                l.setPitch(e.getPlayer().getLocation().getPitch());
                e.getPlayer().teleport(l);
            }
        }
        
    }

    /**
     * Code for world border teleportation behind the border stuff
     *
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {

        if (GameManager.getInstance().isPlayerActive(e.getPlayer())) {
            if (e.isCancelled()) {
                return;
            }

            Integer gid = GameManager.getInstance().getPlayerGameId(e.getPlayer());
            Game game = GameManager.getInstance().getGame(gid);

            if (game.isBorderActive() && game.isDeathmatch()) {

                Location playerLocation = e.getPlayer().getLocation();

                World world = e.getPlayer().getWorld();

                HashMap<String, Location> border = game.getBorders();

                if (border == null) {
                    return;
                }

                Double[] newXZ;

                newXZ = game.checkBorder(playerLocation, border, 0);

                if (newXZ == null) {
                    return;
                }

                newXZ[0] = newXZ[0] == null ? playerLocation.getX() : newXZ[0];
                newXZ[1] = newXZ[1] == null ? playerLocation.getZ() : newXZ[1];

                Double newY = game.goUpUntilFreeSpot(new Location(world, newXZ[0], e.getPlayer().getLocation().getY(), newXZ[1]));

                e.getPlayer().teleport(
                        new Location(e.getPlayer().getWorld(), newXZ[0], newY, newXZ[1]));

                MessageManager.getInstance().sendMessage(MessageManager.PrefixType.WARNING, "You are not allowed to leave the cornucopia! It is time for a deathmatch!", e.getPlayer());
            }
        }
    }
}
