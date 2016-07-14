package org.mcsg.survivalgames.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.SurvivalGames;

public class ChestRatioStorage {

    HashMap<Integer, ArrayList<ItemStack>> lvlstore = new HashMap<Integer, ArrayList<ItemStack>>();
    public static ChestRatioStorage instance = new ChestRatioStorage();
    private int ratio = 2;
    private int maxlevel = 0;

    private ChestRatioStorage() {
    }

    public static ChestRatioStorage getInstance() {
        return instance;
    }

    /**
     * #Modification 6/22/2016 by Keiaxx
     *
     * Recoded chest manager to allow use of item meta.d
     *
     */
    public void setup() {

        YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(GameManager.getInstance().getPlugin().getDataFolder(), "chest.yml"));

        for (int clevel = 1; clevel <= 16; clevel++) {

            ArrayList<ItemStack> lvl = new ArrayList<ItemStack>();

            List<ItemStack> list = (List<ItemStack>) c.getList("chest.lvl" + clevel);

            try {
                if (!list.isEmpty()) {

                    ItemStack[] content = ((List<ItemStack>) c.get("chest.lvl" + clevel)).toArray(new ItemStack[0]);

                    lvl.addAll(Arrays.asList(content));

                    for (ItemStack iStack : content) {

                        SurvivalGames.debug("SG DEBUG: Added item to ChestRatioStorage Level: " + clevel + " Item: " + iStack.getType().name());

                    }

                    lvlstore.put(clevel, lvl);

                } else {
                    SurvivalGames.debug("LIST IS EMPTY " + clevel);
                    maxlevel = clevel - 1;
                    break;
                }
            } catch (NullPointerException ex) {
                
                maxlevel = clevel - 1;
                SurvivalGames.debug("NULL DUDE " + clevel + "MAX LEVEL IS: "+maxlevel);
                break;
                
            }
        }

        ratio = c.getInt("chest.ratio", ratio);

    }

    public int getLevel(int base) {
        Random rand = new Random();
        int max = Math.min(base + 5, maxlevel);
        while (rand.nextInt(ratio) == 0 && base < max) {
            base++;
        }
        return base;
    }

    public ArrayList<ItemStack> getItems(int level) {
        Random r = new Random();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();

        for (int a = 0; a < r.nextInt(7) + 10; a++) {
            if (r.nextBoolean() == true) {
                while (level < level + 5 && level < maxlevel && r.nextInt(ratio) == 1) {
                    level++;
                }

                ArrayList<ItemStack> lvl = lvlstore.get(level);

                ItemStack item = lvl.get(r.nextInt(lvl.size()));

                items.add(item);

            }

        }
        return items;
    }
}
