package org.mcsg.survivalgames.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mcsg.survivalgames.CompassHandler;
import org.mcsg.survivalgames.Game;
import org.mcsg.survivalgames.MessageManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;

public class CompassListener
  implements Runnable
{
  Game plugin;
  int taskID = -1;
  Player player;
  Player target;
  Location originalCompassTarget;
  private final MessageManager msgmgr = MessageManager.getInstance();

  
  public CompassListener(Game instance, Player player, Player target)
  {
    this.plugin = instance;
    this.player = player;
    this.target = target;
    this.originalCompassTarget = player.getCompassTarget();
    CompassHandler.getInstance().addTracker(player, this);
  }
  
  public void setTaskID(int id)
  {
    this.taskID = id;
  }
  
  public void run()
  {
    if (this.target.getLocation().getWorld() != this.player.getLocation().getWorld())
    {
      msgmgr.sendMessage(PrefixType.INFO, this.target.getName()+" cannot be found! Player has either died or left the arena!", player);
      stop();
    }
    else
    {
      this.player.setCompassTarget(this.target.getLocation());
    }
    
  }
  
  public void stop()
  {
    this.player.setCompassTarget(this.originalCompassTarget);
    
    Bukkit.getServer().getScheduler().cancelTask(this.taskID);
    CompassHandler.getInstance().removeTracker(this.player);
  }
  
  public void disable()
  {
    msgmgr.sendMessage(PrefixType.INFO,  "Player tracker disabled. Right click with your compass to enable.", player);
    this.player.setCompassTarget(this.originalCompassTarget);
    Bukkit.getServer().getScheduler().cancelTask(this.taskID);
    CompassHandler.getInstance().removeTracker(this.player);
  }
}
