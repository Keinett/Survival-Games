package org.mcsg.survivalgames.points;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.mcsg.survivalgames.SettingsManager;
import org.mcsg.survivalgames.SurvivalGames;

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

        if (cachedStats.containsKey(player)) {
            return cachedStats.get(player.toLowerCase());
        } else {
            return null;
        }

    }

    public void playerQuery(final String query) {
        if (PointSystem.getInstance().mysqlReady()) {
            try {
                PointSystem.getInstance().getPlayerConnection().executeQuery(query, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void arenaQuery(final String query) {
        if (PointSystem.getInstance().mysqlReady()) {
            try {
                PointSystem.getInstance().getArenaConnection().executeQuery(query, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean mysqlExists(final String query) {
        ResultSet result = null;
        if (PointSystem.getInstance().mysqlReady()) {
            try {
                result = PointSystem.getInstance().getPlayerConnection().executeQuery(query, false);
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

        int addFactor = SettingsManager.getInstance().getConfig().getInt("stats.win.addFactor");
        int multiFactor =  SettingsManager.getInstance().getConfig().getInt("stats.win.multiFactor");
        double ratio = SettingsManager.getInstance().getConfig().getDouble("stats.win.ratio");
        
        Double calcScale = endtime.doubleValue() / totaltime.doubleValue();

        Double calcMaxScore = addFactor + (multiFactor * (numPlayers * ratio)); // max number of points player can win

        return (int) Math.ceil(calcMaxScore * calcScale); //Points player wins
    }

    public void saveGame(Integer arena, ArrayList<String> players, String winner, Date end, Date start, Integer endtime, Integer totaltime) {

        int playerSize = players.size();

        int points = calculatePoints(totaltime, endtime, playerSize);

        long seconds = (end.getTime() - start.getTime()) / 1000;

        String playerList = String.join("|", players);

        Integer time = (int) seconds;

        arenaQuery("INSERT INTO `" + PointSystem.getInstance().getArenaTable() + "` (`arena`,`players`,`winner`,`points`,`gameLength`,`gameDate`) VALUES ('" + arena + "', '" + playerList + "', '" + winner + "', '" + points + "', '" + time + "', NOW())");

    }

    public void addWin(final String player, Integer endtime, Integer totaltime, Integer startplayers, Integer gameLength) {

        int calcPoints = calculatePoints(totaltime, endtime, startplayers);

        playerQuery("UPDATE `" + PointSystem.getInstance().getPlayerTable() + "`SET `playtime` = `playtime`+" + gameLength + ", `win` = `win`+1 WHERE `name` = '" + player + "'");

        addPoints(player.toLowerCase(), calcPoints);

        PlayerStats ps = cachedStats.get(player.toLowerCase());
        ps.addWin();
        ps.addPlaytime(gameLength);

    }

    public void addKill(final String player, final Integer playtime) {

        int points = SettingsManager.getInstance().getConfig().getInt("stats.player.kill");

        playerQuery("UPDATE `" + PointSystem.getInstance().getPlayerTable() + "` SET `kills` = `kills`+1 WHERE `name` = '" + player + "'");

        addPoints(player.toLowerCase(), points);

        PlayerStats ps = cachedStats.get(player.toLowerCase());
        ps.addKill();

    }

    public void addDeath(final String player, final Integer playtime) {

        int points = SettingsManager.getInstance().getConfig().getInt("stats.player.death");

        playerQuery("UPDATE `" + PointSystem.getInstance().getPlayerTable() + "` SET `playtime` = `playtime`+" + playtime + ", `deaths` = `deaths`+1 WHERE `name` = '" + player + "'");

        PlayerStats ps = cachedStats.get(player.toLowerCase());
        ps.addDeath();
        ps.addPlaytime(playtime);
    }

    public void addPoints(String player, Integer points) {

        points = (int) Math.round(points);

        playerQuery("UPDATE `" + PointSystem.getInstance().getPlayerTable() + "` SET `points` = `points`+" + points + " WHERE `name` = '" + player + "'");

        PlayerStats ps = cachedStats.get(player.toLowerCase());
        ps.addPoints(points);

    }

    /**
     * Called upon player join to initialize player data if does not exist
     *
     * @param player
     */
    public void initPlayer(final String player) {

        if (!cachedStats.containsKey(player.toLowerCase())) {
            playerQuery("INSERT INTO `" + PointSystem.getInstance().getPlayerTable() + "` (`name`, `kills`, `deaths`, `points`, `win`, `playtime`) VALUES ('"
                    + player + "', 0, 0, 0, 0, 0)");

            String lowerName = player.toLowerCase();

            PlayerStats ps = new PlayerStats(lowerName, 0, 0, 0, 0, 0);

            cachedStats.put(lowerName, ps);

        }
    }

    public void updateAllRanks() {
        for (PlayerStats ps : cachedStats.values()) {
            ps.updateRank();
        }
    }

    public void initStatCache() {
        long start_time = System.nanoTime();
        double difference = 0;

        if (!PointSystem.getInstance().mysqlReady()) {
            SurvivalGames.debug("MySQL is not set!");
            cachedStats = null;
        } else {
            ResultSet result = null;
            try {
                result = PointSystem.getInstance().getPlayerConnection()
                        .executeQuery("SELECT * FROM `" + PointSystem.getInstance().getPlayerTable() + "` WHERE 1", false);
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

            updateAllRanks();

            long end_time = System.nanoTime();
            difference = (end_time - start_time) / 1e6;
            SurvivalGames.debug("Stats initialization/update took " + difference + "ms to update.");
        }

    }

}
