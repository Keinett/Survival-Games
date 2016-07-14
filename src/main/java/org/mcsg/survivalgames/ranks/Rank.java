/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mcsg.survivalgames.ranks;

import net.md_5.bungee.api.ChatColor;

/**
 *
 * @author Administrator
 */
public class Rank {
    private final String name;
    private final int neededPoints;
    private final String prefix;
    private final int shopUnlockVal;
    
    public Rank(String name, int points, String prefix, int unlock){
        this.name = name;
        this.neededPoints = points;
        this.prefix = prefix;
        this.shopUnlockVal = unlock;
    }
    
    public String getName(){
        return name;
    }
    
    public int getPoints(){
        return neededPoints;
    }
    
    public String getPrefix(){
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }
    
    public int getUnlockValue(){
        return shopUnlockVal;
    }
}
