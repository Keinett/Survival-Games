package org.mcsg.survivalgames.points;

import org.mcsg.survivalgames.ranks.Rank;
import org.mcsg.survivalgames.ranks.RankManager;

public class PlayerStats {

    private final String name;
    private int kills;
    private int wins;
    private int deaths;
    private int playtime;
    private int points;
    private Rank rank;
    

    public PlayerStats(String name, int kills, int wins, int deaths, int points, int playtime) {
        this.name = name;
        this.kills = kills;
        this.wins = wins;
        this.deaths = deaths;
        this.points = points;
        this.playtime = playtime;
        this.rank = RankManager.getInstance().getRank(points);
    }
    
    public void updateRank(){
        rank = RankManager.getInstance().getRank(points);
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addWin() {
        wins++;
    }

    public void addPlaytime(Integer i) {
        playtime = playtime + i;
    }

    public void addPoints(Integer i) {
        points = points + i;
    }

    public String getName() {
        return name;
    }
    
    public String getRankName(){
        return rank.getName();
    }
    
    public String getRankPrefix(){
        return rank.getPrefix();
    }

    public int getWins() {
        return wins;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getPoints() {
        return points;
    }

    public int getPlaytimeSeconds() {
        return playtime;
    }

    public String getFormattedPlaytime() {
        String formatted = null;

        int hours = 0;

        if (!(playtime < 3600)) {
            hours = playtime / 3600;
        }

        int minutes = (playtime % 3600) / 60;

        if (hours == 1 && minutes != 1) {
            
            formatted = hours + " hour " + minutes + " minutes";
            
        } else if (hours != 1 && minutes != 1) {

            formatted = hours + " hours " + minutes + " minutes";

        } else if (hours != 1 && minutes == 1) {

            formatted = hours + " hours " + minutes + " minute";

        } else {

            formatted = hours + " hour " + minutes + " minute";
        }

        return formatted;
    }

    public int getGamesPlayed() {
        return kills + deaths;
    }

    public Double getKDA() {
        double d = (double) kills / (double) deaths;
        return d;
    }

}
