/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mcsg.survivalgames.ranks;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.mcsg.survivalgames.SurvivalGames;

/**
 *
 * @author Administrator
 */
public class RankManager {

    public static RankManager instance = new RankManager();

    public static HashMap<Integer, Rank> rankList = new HashMap<>();

    public RankManager() {

    }

    public void initializeRanks(FileConfiguration file) {

        int numRanks = file.getInt("ranks.numRanks");

        for (int i = 1; i <= numRanks; i++) {

            String name = file.getString("ranks." + i + ".name");
            int points = file.getInt("ranks." + i + ".points");
            String prefix = file.getString("ranks." + i + ".prefix");
            int unlockValue = file.getInt("ranks." + i + ".unlockvalue");

            Rank r = new Rank(name, points, prefix, unlockValue);

            rankList.put(i, r);

            SurvivalGames.debug("Loaded rank " + name + " as index " + i);

        }
    }

    public Rank getRank(int points) {

        int numRanks = rankList.size();

        SurvivalGames.debug("Number of ranks: " + numRanks + "");

        // Plugin News 7/6/2016: Rushnett and I (Keiaxx) spent a good 30 minutes
        // debugging the rank system because I did '>' instead of '<'
        // RIP time lost
        if (numRanks < 2) {
            return rankList.get(1);
        }

        int firstRank = 1;
        int secondRank = 2;

        Rank isRank = rankList.get(1);

        for (int i = 1; i <= numRanks; i++) {

            int firstPoints = 0;
            int secondPoints = 0;
            try {
                firstPoints = rankList.get(firstRank).getPoints();
                secondPoints = rankList.get(secondRank).getPoints();
            } catch (NullPointerException e) {
            }

            if (points >= firstPoints && points < secondPoints) {
                isRank = rankList.get(firstRank);
            } else if (firstRank == numRanks) {
                isRank = rankList.get(firstRank);
            } else {
                firstRank++;
                secondRank++;
            }
        }
        return isRank;
    }

    public static RankManager getInstance() {
        return instance;
    }

}
