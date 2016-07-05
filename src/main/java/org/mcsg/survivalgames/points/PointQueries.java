package org.mcsg.survivalgames.points;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;

import org.mcsg.survivalgames.SettingsManager;

/**
 *
 * @version v0.1.0
 *
 * @author slipcor
 *
 */
public final class PointQueries {

    public static ConcurrentHashMap<String, PlayerStats> cachedStats;

    public PointQueries() {

    }

    public static PlayerStats getStats(String player) {
        
        if(cachedStats.containsKey(player)){
           return cachedStats.get(player.toLowerCase()); 
        }else{
           return null;
        }
        
    }

    public void mysqlQuery(final String query) {
        if (PointSystem.getInstance().mySQL) {
            try {
                PointSystem.getInstance().playerStatHandler.executeQuery(query, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void mysqlQuery2(final String query) {
        if (PointSystem.getInstance().mySQL) {
            try {
                PointSystem.getInstance().arenaStatHandler.executeQuery(query, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean mysqlExists(final String query) {
        ResultSet result = null;
        if (PointSystem.getInstance().mySQL) {
            try {
                result = PointSystem.getInstance().playerStatHandler.executeQuery(query, false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            while (result != null && result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Integer calculatePoints(Integer totaltime, Integer endtime, Integer numPlayers) {

        Double calcScale = endtime.doubleValue() / totaltime.doubleValue();

        Double calcMaxScore = 4 + (4 * (numPlayers * .27)); // max number of points player can win

        return (int) Math.ceil(calcMaxScore * calcScale); //Points player wins
    }

    public void saveGame(Integer arena, ArrayList<String> players, String winner, Date end, Date start, Integer endtime, Integer totaltime) {

        int playerSize = players.size();

        int points = calculatePoints(totaltime, endtime, playerSize);

        long seconds = (end.getTime() - start.getTime()) / 1000;

        String playerList = String.join("|", players);

        Integer time = (int) seconds;

        mysqlQuery2("INSERT INTO `" + PointSystem.getInstance().arenaStatTable + "` (`arena`,`players`,`winner`,`points`,`gameLength`,`gameDate`) VALUES ('" + arena + "', '" + playerList + "', '" + winner + "', '" + points + "', '" + time + "', NOW())");

    }

    public void addKill(final String player, final Integer playtime, final Integer points) {

        FileConfiguration c = SettingsManager.getInstance().getConfig();
        mysqlQuery("UPDATE `" + PointSystem.getInstance().playerStatTable + "` SET `kills` = `kills`+1 WHERE `name` = '" + player + "'");

        addPoints(player, points);

        cachedStats.get(player).addKill();
    }

    public void addWin(final String player, Integer endtime, Integer totaltime, Integer startplayers, Integer gameLength) {

        int calcPoints = calculatePoints(totaltime, endtime, startplayers);

        mysqlQuery("UPDATE `" + PointSystem.getInstance().playerStatTable + "`SET `playtime` = `playtime`+" + gameLength + ", `win` = `win`+1 WHERE `name` = '" + player + "'");

        addPoints(player, calcPoints);

        cachedStats.get(player).addWin();
    }

    public void addDeath(final String player, final Integer playtime) {

        mysqlQuery("UPDATE `" + PointSystem.getInstance().playerStatTable + "` SET `playtime` = `playtime`+" + playtime + ", `deaths` = `deaths`+1 WHERE `name` = '" + player + "'");

        cachedStats.get(player).addDeath();
    }

    public void addPoints(String player, Integer points) {

        points = (int) Math.round(points);

        mysqlQuery("UPDATE `" + PointSystem.getInstance().playerStatTable + "` SET `points` = `points`+" + points + " WHERE `name` = '" + player + "'");

        cachedStats.get(player).addPoints(points);

    }

    /**
     * Called upon player join to initialize player data if does not exist
     *
     * @param player
     */
    public void initPlayer(final String player) {

        if (!cachedStats.containsKey(player.toLowerCase())) {
            mysqlQuery("INSERT INTO `" + PointSystem.getInstance().playerStatTable + "` (`name`, `kills`, `deaths`, `points`, `win`, `playtime`) VALUES ('"
                    + player + "', 0, 0, 0, 0, 0)");

            String lowerName = player.toLowerCase();

            PlayerStats ps = new PlayerStats(lowerName, 0, 0, 0, 0, 0);

            cachedStats.put(lowerName, ps);
        }
    }

    public void initStatCache() {
        long start_time = System.nanoTime();
        double difference = 0;

        if (!PointSystem.getInstance().mySQL) {
            System.out.println("[SurvivalGames] MySQL is not set!");
            cachedStats = null;
        } else {
            ResultSet result = null;
            try {
                result = PointSystem.getInstance().playerStatHandler
                        .executeQuery("SELECT * FROM `" + PointSystem.getInstance().playerStatTable + "` WHERE 1", false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            cachedStats = new ConcurrentHashMap<>();
            try {
                while (result != null && result.next()) {
                    //public PlayerStats(String name, int kills, int wins, int deaths, int points, int playtime) {

                    String name = result.getString("name").toLowerCase();
                    int kills = result.getInt("kills");
                    int deaths = result.getInt("deaths");
                    int wins = result.getInt("win");
                    int points = result.getInt("points");
                    int playTime = result.getInt("playtime");

                    PlayerStats ps = new PlayerStats(name, kills, wins, deaths, points, playTime);

                    cachedStats.put(name, ps);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            long end_time = System.nanoTime();
            difference = (end_time - start_time) / 1e6;
            System.out.println("[SurvivalGames] Stats initialization/update took " + difference + "ms to update.");
        }

    }

}
