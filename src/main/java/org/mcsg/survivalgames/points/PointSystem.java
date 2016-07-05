/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mcsg.survivalgames.points;

import java.sql.SQLException;
import org.bukkit.configuration.file.FileConfiguration;
import org.mcsg.survivalgames.SettingsManager;
import org.mcsg.survivalgames.SurvivalGames;

public class PointSystem {

    private static PointSystem pSystem = new PointSystem();

    private static PointQueries statSQL = new PointQueries();
    

    public Boolean mySQL = false;

    public String dbHost = null;
    public int dbPort = 3306;
    public String dbUser = null;
    public String dbPass = null;
    public String dbDatabase = null;

    public String playerStatTable = null;
    public String arenaStatTable = null;

    public MySQLConnection playerStatHandler; // MySQL handler stats
    public MySQLConnection arenaStatHandler; // MySQL handler svegames

    private PointSystem() {

    }

    public void setup(SurvivalGames p) {

        FileConfiguration conf = SettingsManager.getInstance().getConfig();

        // get variables from settings handler
        if (conf.getBoolean("points.enabled")) {
            this.mySQL = true;
            this.dbHost = conf.getString("sql.host");
            this.dbUser = conf.getString("sql.user");
            this.dbPass = conf.getString("sql.pass");
            this.dbDatabase = conf.getString("sql.database");
            this.playerStatTable = conf.getString("sql.prefix") + "pvpstats";
            this.arenaStatTable = conf.getString("sql.prefix") + "arenastats";
            this.dbPort = conf.getInt("sql.port");
        }

        // Check Settings
        if (this.mySQL) {
            if (this.dbHost.equals("")) {
                this.mySQL = false;
            } else if (this.dbUser.equals("")) {
                this.mySQL = false;
            } else if (this.dbPass.equals("")) {
                this.mySQL = false;
            } else if (this.dbDatabase.equals("")) {
                this.mySQL = false;
            }
        }

        // Enabled SQL/MySQL
        if (this.mySQL) {
            // Declare MySQL Handler
            try {
                playerStatHandler = new MySQLConnection(playerStatTable, dbHost, dbPort, dbDatabase, dbUser,
                        dbPass);

                arenaStatHandler = new MySQLConnection(arenaStatTable, dbHost, dbPort, dbDatabase, dbUser,
                        dbPass);
            } catch (InstantiationException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }

            System.out.println("MySQL Initializing");
            // Initialize MySQL Handler

            if (playerStatHandler.connect(true)) {
                System.out.println("MySQL connection successful");
                // Check if the tables exist, if not, create them
                if (!playerStatHandler.tableExists(dbDatabase, playerStatTable)) {
                    System.out.println("Creating table " + playerStatTable);
                    final String query = "CREATE TABLE `" + playerStatTable + "` ( `id` int(5) NOT NULL AUTO_INCREMENT,"
                            + " `name` varchar(42) NOT NULL,"
                            + " `kills` int(8) not null default 0,"
                            + " `deaths` int(8) not null default 0,"
                            + " `points` int(8) not null default 0,"
                            + " `win` int(8) not null default 0,"
                            + " `playtime` int(8) not null default 0,"
                            + " PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
                    try {
                        playerStatHandler.executeQuery(query, true);
                        System.out.println("Table Created");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {

                }
            } else {
                System.out.println("MySQL connection failed");
                this.mySQL = false;
            }

            if (arenaStatHandler.connect(true)) {
                System.out.println("MySQL connection successful");
                // Check if the tables exist, if not, create them
                if (!arenaStatHandler.tableExists(dbDatabase, arenaStatTable)) {

                    System.out.println("Creating table " + arenaStatTable);

                    final String query = "CREATE TABLE `" + arenaStatTable + "` ( `id` int(5) NOT NULL AUTO_INCREMENT,"
                            + " `arena` int(8) not null default 0,"
                            + " `players` text NOT NULL,"
                            + " `winner` varchar(42) NOT NULL,"
                            + " `points` int(8) not null default 0,"
                            + " `gameLength` int(8) not null default 0,"
                            + " `gameDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                            + " PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
                    try {
                        arenaStatHandler.executeQuery(query, true);
                        System.out.println("Table Created");
                    } catch (SQLException e) {
                    }

                } else {

                }
            } else {
                System.out.println("MySQL connection failed");
                this.mySQL = false;
            }

            pSystem = this;
        }
    }

    public static PointQueries getQueryHandler() {
        return statSQL;
    }

    public static PointSystem getInstance() {
        return pSystem;
    }
}
