/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mcsg.survivalgames;

import java.util.HashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.mcsg.survivalgames.util.CompassListener;

/**
 *
 * @author Adrian Gose
 */
public class CompassHandler {
    
    FileConfiguration config;
    static CompassHandler instance;
    
    Double radius = 100D;   
    Boolean compassEnabled = true;
    public HashMap<Player, CompassListener> trackers = new HashMap();
    
    
    
    public void setuo(){
        CompassHandler.instance = this;
        config = SettingsManager.getInstance().getConfig();
        // Set up Compass Stuff
        radius = config.getDouble("compass.radius");
        compassEnabled = config.getBoolean("compass.enabled");
        trackers.clear();
    }
    
    
    public void stopAllCompassListeners(){
       for(CompassListener listeners : this.trackers.values()){
           listeners.stop();
       } 
    }
    
    public void stopPlayerCompassListener(Player p){
        CompassListener cl = trackers.get(p);
        cl.stop();
    }
    
    public void addTracker(Player p, CompassListener l){
        this.trackers.put(p, l);
    }
    
    public void removeTracker(Player p){
        this.trackers.remove(p);
    }
    
    public boolean containsTracker(Player p){
        return trackers.containsKey(p);
    }
    
    public CompassListener getCompassListener(Player p){
        return trackers.get(p);
    }
    
    public double getCompassRadius(){
        return radius;
    }
    
    public static CompassHandler getInstance(){
        return instance;
    }
    
}
