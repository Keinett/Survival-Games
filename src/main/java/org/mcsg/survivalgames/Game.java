package org.mcsg.survivalgames;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.mcsg.survivalgames.MessageManager.PrefixType;
import org.mcsg.survivalgames.api.PlayerJoinArenaEvent;
import org.mcsg.survivalgames.api.PlayerKilledEvent;
import org.mcsg.survivalgames.api.PlayerLeaveArenaEvent;
import org.mcsg.survivalgames.hooks.HookManager;
import org.mcsg.survivalgames.logging.QueueManager;
import org.mcsg.survivalgames.points.PointSystem;
import org.mcsg.survivalgames.stats.StatsManager;
import org.mcsg.survivalgames.util.ItemReader;
import org.mcsg.survivalgames.util.Kit;

//Data container for a game
public class Game {

    public static enum GameMode {

        DISABLED, LOADING, INACTIVE, WAITING, STARTING, INGAME, FINISHING, RESETING, ERROR
    }

    private GameMode mode = GameMode.DISABLED;
    private ArrayList<String> startingPlayers = new ArrayList<String>();
    private ArrayList<Player> activePlayers = new ArrayList<Player>();
    private ArrayList<Player> inactivePlayers = new ArrayList<Player>();
    private ArrayList< String> spectators = new ArrayList< String>();
    private ArrayList<Player> queue = new ArrayList<Player>();
    private HashMap<String, Object> flags = new HashMap<String, Object>();
    HashMap<Player, Integer> nextspec = new HashMap<Player, Integer>();
    private ArrayList<Integer> tasks = new ArrayList<Integer>();

    private Arena arena;
    private Integer gameID;
    private Integer gcount = 0;
    private FileConfiguration config;
    private FileConfiguration system;
    private HashMap< Integer, Player> spawns = new HashMap< Integer, Player>();
    private HashMap<Player, ItemStack[][]> inv_store = new HashMap<Player, ItemStack[][]>();
    private Integer spawnCount = 0;
    private Integer vote = 0;
    private boolean disabled = false;
    private Integer endgameTaskID = 0;
    private boolean endgameRunning = false;
    private double rbpercent = 0;
    private String rbstatus = "";
    private long startTime = 0;
    private boolean countdownRunning;
    private StatsManager sm = StatsManager.getInstance();
    private HashMap<String, String> hookvars = new HashMap<String, String>();
    private MessageManager msgmgr = MessageManager.getInstance();

    /* Scoreboard Stuff - Keiaxx */
    private ScoreboardManager manager;
    private Scoreboard gameBoard;
    private Objective gameObj;
    private Objective healthObj;
    private Integer boardTaskID = 0;

    /* Game length time managment */
    private Date startDate;
    private Date endDate;
    private Integer gameLengthSeconds = 1800;
    private Integer gameElapsedSeconds = 0;
    final private Integer maximumPointsTime = 1800; // The time it takes until the maximum amount of points can be given
    private Integer elapsedPointsTime = 0;

    /* Deathmatch */
    private Boolean deathmatchEnabled = true;
    public Integer countdownTaskID = 0;
    public boolean inDeathmatch = false;
    private Integer numStartPlayers = 0;
    private Boolean isPvpEnabled = true;
    private Boolean dmCountdownRunning = false;

    /* Deathmatch Border */
    private Location rectPoint1;
    private Location rectPoint2;
    public boolean isBorderActive;
    public static final double buffer = 0.5;
    private HashMap<String, Location> borders = new HashMap<>();

    public Game(int gameid) {
        gameID = gameid;
        reloadConfig();
        setup();
    }

    public void reloadConfig() {
        config = SettingsManager.getInstance().getConfig();
        system = SettingsManager.getInstance().getSystemConfig();
    }

    public void $(String msg) {
        SurvivalGames.$(msg);
    }

    public void debug(String msg) {
        SurvivalGames.debug(msg);
    }

    public void setup() {
        mode = GameMode.LOADING;

        int x = system.getInt("sg-system.arenas." + gameID + ".x1");
        int y = system.getInt("sg-system.arenas." + gameID + ".y1");
        int z = system.getInt("sg-system.arenas." + gameID + ".z1");
        $(x + " " + y + " " + z);
        int x1 = system.getInt("sg-system.arenas." + gameID + ".x2");
        int y1 = system.getInt("sg-system.arenas." + gameID + ".y2");
        int z1 = system.getInt("sg-system.arenas." + gameID + ".z2");
        $(x1 + " " + y1 + " " + z1);
        Location max = new Location(SettingsManager.getGameWorld(gameID), Math.max(x, x1), Math.max(y, y1), Math.max(z, z1));
        $(max.toString());
        Location min = new Location(SettingsManager.getGameWorld(gameID), Math.min(x, x1), Math.min(y, y1), Math.min(z, z1));
        $(min.toString());

        arena = new Arena(min, max);

        loadspawns();

        manager = Bukkit.getScoreboardManager();
        gameBoard = manager.getNewScoreboard();
        gameObj = gameBoard.registerNewObjective("arena" + gameID, "dummy");

        gameObj.setDisplayName("Game Stats");

        gameObj.setDisplaySlot(DisplaySlot.SIDEBAR);

        gameBoard.registerNewObjective("showhealth", "health");
        healthObj = gameBoard.getObjective("showhealth");
        healthObj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        healthObj.setDisplayName("/ 20");

        hookvars.put("arena", gameID + "");
        hookvars.put("maxplayers", spawnCount + "");
        hookvars.put("activeplayers", "0");

        gameLengthSeconds = config.getInt("deathmatch.time") * 60; // Multiply the given minutes by 60 to get length in seconds

        deathmatchEnabled = config.getBoolean("deathmatch.enabled");

        mode = GameMode.WAITING;
    }

    /*
     * 
     * ################################################
     * 
     *              SCOREBOARD STUFF
     * 
     * ################################################
     * 
     * 
     */
    public void initBoard(Player p) {
        sendScore(gameObj, ChatColor.GREEN + "Alive:", getActivePlayers(), false);
        sendScore(gameObj, ChatColor.RED + "Dead:", getInactivePlayers(), false);
        sendScore(gameObj, ChatColor.YELLOW + "Spectators:", getSpectatingPlayers(), false);

        if (deathmatchEnabled) {
            int length = (int) gameLengthSeconds / 60;
            sendScore(gameObj, ChatColor.YELLOW + "Time Left:", length, false);
        }

        p.setScoreboard(gameBoard);
    }

    public void updateObjectives() {
        sendScore(gameObj, ChatColor.GREEN + "Alive:", getActivePlayers(), false);
        sendScore(gameObj, ChatColor.RED + "Dead:", getInactivePlayers(), false);
        sendScore(gameObj, ChatColor.YELLOW + "Spectators:", getSpectatingPlayers(), false);

        if (deathmatchEnabled) {
            int length = (int) gameLengthSeconds / 60;
            sendScore(gameObj, ChatColor.YELLOW + "Time Left:", length, false);
        }
    }

    public void sendScore(Objective objective, String title, int value, Boolean fupdate) {
        Score score = objective.getScore(Bukkit.getOfflinePlayer(title));
        if (fupdate) {
            score.setScore(-1);
        }
        score.setScore(value);
    }

    public void stopBoardUpdater() {

        Bukkit.getScheduler().cancelTask(boardTaskID);
        debug("Stopping board updater for arena " + getName() + " task ID :" + boardTaskID);

    }

    public void startBoardUpdater() {
        boardTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {
                updateObjectives();

                if ((getActivePlayers() == 0 && mode == GameMode.INGAME) || (getActivePlayers() == 0 && mode == GameMode.STARTING) || (getActivePlayers() == 0 && mode == GameMode.FINISHING)) {
                    endGame();
                }
            }
        }, 0, 10);
    }

    public void removeScoreboard(final Player p) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }, 20L);
    }

    /* End Scoreboard Stuff */
    public void reloadFlags() {
        flags = SettingsManager.getInstance().getGameFlags(gameID);
    }

    public void saveFlags() {
        SettingsManager.getInstance().saveGameFlags(flags, gameID);
    }

    public void loadspawns() {
        for (int a = 1; a <= SettingsManager.getInstance().getSpawnCount(gameID); a++) {
            spawns.put(a, null);
            spawnCount = a;
        }
    }

    public void addSpawn() {
        spawnCount++;
        spawns.put(spawnCount, null);
    }

    public void setMode(GameMode m) {
        mode = m;
    }

    public GameMode getGameMode() {
        return mode;
    }

    public Arena getArena() {
        return arena;
    }

    /*
     * 
     * ################################################
     * 
     * ENABLE
     * 
     * ################################################
     * 
     * 
     */
    public void enable() {
        mode = GameMode.WAITING;
        if (disabled) {
            MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gameenabled", "arena-" + gameID);
        }
        disabled = false;
        int b = (SettingsManager.getInstance().getSpawnCount(gameID) > queue.size()) ? queue.size() : SettingsManager.getInstance().getSpawnCount(gameID);
        for (int a = 0; a < b; a++) {
            addPlayer(queue.remove(0));
        }
        int c = 1;
        for (Player p : queue) {
            msgmgr.sendMessage(PrefixType.INFO, "You are now #" + c + " in line for arena " + gameID, p);
            c++;
        }

        LobbyManager.getInstance().updateWall(gameID);

        MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gamewaiting", "arena-" + gameID);

    }

    /*
     * 
     * ################################################
     * 
     * ADD PLAYER
     * 
     * ################################################
     * 
     * 
     */
    public boolean addPlayer(Player p) {
        if (SettingsManager.getInstance().getLobbySpawn() == null) {
            msgmgr.sendFMessage(PrefixType.WARNING, "error.nolobbyspawn", p);
            return false;
        }
        if (!p.hasPermission("sg.arena.join." + gameID)) {
            debug("permission needed to join arena: " + "sg.arena.join." + gameID);
            msgmgr.sendFMessage(PrefixType.WARNING, "game.nopermission", p, "arena-" + gameID);
            return false;
        }
        HookManager.getInstance().runHook("GAME_PRE_ADDPLAYER", "arena-" + gameID, "player-" + p.getName(), "maxplayers-" + spawns.size(), "players-" + activePlayers.size());

        GameManager.getInstance().removeFromOtherQueues(p, gameID);

        if (GameManager.getInstance().getPlayerGameId(p) != -1) {
            if (GameManager.getInstance().isPlayerActive(p)) {
                msgmgr.sendMessage(PrefixType.ERROR, "Cannot join multiple games!", p);
                return false;
            }
        }
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        if (spectators.contains(p)) {
            removeSpectator(p);
        }
        if (mode == GameMode.WAITING || mode == GameMode.STARTING) {
            if (activePlayers.size() < SettingsManager.getInstance().getSpawnCount(gameID)) {
                msgmgr.sendMessage(PrefixType.INFO, "Joining Arena " + gameID, p);
                PlayerJoinArenaEvent joinarena = new PlayerJoinArenaEvent(p, GameManager.getInstance().getGame(gameID));
                Bukkit.getServer().getPluginManager().callEvent(joinarena);
                if (joinarena.isCancelled()) {
                    return false;
                }
                boolean placed = false;
                int spawnCount2 = SettingsManager.getInstance().getSpawnCount(gameID);

                for (int a = 1; a <= spawnCount2; a++) {
                    if (spawns.get(a) == null) {
                        placed = true;
                        spawns.put(a, p);
                        p.setGameMode(org.bukkit.GameMode.SURVIVAL);

                        p.teleport(SettingsManager.getInstance().getLobbySpawn());
                        saveInv(p);
                        clearInv(p);
                        p.teleport(SettingsManager.getInstance().getSpawnPoint(gameID, a));

                        p.setHealth(p.getMaxHealth());
                        p.setFoodLevel(20);
                        clearInv(p);

                        activePlayers.add(p);
                        sm.addPlayer(p, gameID);

                        hookvars.put("activeplayers", activePlayers.size() + "");
                        LobbyManager.getInstance().updateWall(gameID);
                        showMenu(p);
                        HookManager.getInstance().runHook("GAME_POST_ADDPLAYER", "activePlayers-" + activePlayers.size());

                        if (spawnCount == activePlayers.size()) {
                            countdown(5);
                        }

                        initBoard(p);

                        break;
                    }
                }
                if (!placed) {
                    msgmgr.sendFMessage(PrefixType.ERROR, "error.gamefull", p, "arena-" + gameID);
                    return false;
                }

            } else if (SettingsManager.getInstance().getSpawnCount(gameID) == 0) {
                msgmgr.sendMessage(PrefixType.WARNING, "No spawns set for Arena " + gameID + "!", p);
                return false;
            } else {
                msgmgr.sendFMessage(PrefixType.WARNING, "error.gamefull", p, "arena-" + gameID);
                return false;
            }
            msgFall(PrefixType.INFO, "game.playerjoingame", "player-" + p.getName(), "activeplayers-" + getActivePlayers(), "maxplayers-" + SettingsManager.getInstance().getSpawnCount(gameID));
            if (activePlayers.size() >= config.getInt("auto-start-players") && !countdownRunning) {
                countdown(config.getInt("auto-start-time"));
            }
            return true;
        } else {
            if (config.getBoolean("enable-player-queue")) {
                if (!queue.contains(p)) {
                    queue.add(p);
                    msgmgr.sendFMessage(PrefixType.INFO, "game.playerjoinqueue", p, "queuesize-" + queue.size());
                }
                int a = 1;
                for (Player qp : queue) {
                    if (qp == p) {
                        msgmgr.sendFMessage(PrefixType.INFO, "game.playercheckqueue", p, "queuepos-" + a);
                        break;
                    }
                    a++;
                }
            }
        }
        if (mode == GameMode.INGAME) {
            msgmgr.sendFMessage(PrefixType.WARNING, "error.alreadyingame", p);
        } else if (mode == GameMode.DISABLED) {
            msgmgr.sendFMessage(PrefixType.WARNING, "error.gamedisabled", p, "arena-" + gameID);
        } else if (mode == GameMode.RESETING) {
            msgmgr.sendFMessage(PrefixType.WARNING, "error.gamereseting", p);
        } else {
            msgmgr.sendMessage(PrefixType.INFO, "Cannot join game!", p);
        }
        LobbyManager.getInstance().updateWall(gameID);
        return false;
    }

    public void showMenu(Player p) {
        GameManager.getInstance().openKitMenu(p);
        Inventory i = Bukkit.getServer().createInventory(p, 90, ChatColor.RED + "" + ChatColor.BOLD + "Kit Selection");

        int a = 0;
        int b = 0;

        ArrayList<Kit> kits = GameManager.getInstance().getKits(p);
        SurvivalGames.debug(kits + "");
        if (kits == null || kits.size() == 0 || !SettingsManager.getInstance().getKits().getBoolean("enabled")) {
            GameManager.getInstance().leaveKitMenu(p);
            return;
        }

        for (Kit k : kits) {
            ItemStack i1 = k.getIcon();
            ItemMeta im = i1.getItemMeta();

            debug(k.getName() + " " + i1 + " " + im);

            im.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + k.getName());
            i1.setItemMeta(im);
            i.setItem((9 * a) + b, i1);
            a = 2;

            for (ItemStack s2 : k.getContents()) {
                if (s2 != null) {
                    i.setItem((9 * a) + b, s2);
                    a++;
                }
            }

            a = 0;
            b++;
        }
        p.openInventory(i);
        debug("Showing menu");
    }

    public void removeFromQueue(Player p) {
        queue.remove(p);
    }

    /*
     * 
     * ################################################
     * 
     * VOTE
     * 
     * ################################################
     * 
     * 
     */
    ArrayList<Player> voted = new ArrayList<Player>();

    public void vote(Player pl) {

        if (GameMode.STARTING == mode) {
            msgmgr.sendMessage(PrefixType.WARNING, "Game already starting!", pl);
            return;
        }
        if (GameMode.WAITING != mode) {
            msgmgr.sendMessage(PrefixType.WARNING, "Game already started!", pl);
            return;
        }
        if (voted.contains(pl)) {
            msgmgr.sendMessage(PrefixType.WARNING, "You already voted!", pl);
            return;
        }
        vote++;
        voted.add(pl);
        msgmgr.sendFMessage(PrefixType.INFO, "game.playervote", pl, "player-" + pl.getName());
        HookManager.getInstance().runHook("PLAYER_VOTE", "player-" + pl.getName());

        for (Player p : activePlayers) {

            msgmgr.sendMessage(PrefixType.INFO, pl.getName() + " voted to start the game! " + vote + "/" + this.getActivePlayers() + " players have voted.", p);

        }

        if ((((vote + 0.0) / (getActivePlayers() + 0.0)) >= (config.getInt("auto-start-vote") + 0.0) / 100) && getActivePlayers() > 1) {
            countdown(config.getInt("auto-start-time"));
            for (Player p : activePlayers) {
                // p.sendMessage(ChatColor.LIGHT_PURPLE + "Game Starting in " +
                // c.getInt("auto-start-time"));
                msgmgr.sendMessage(PrefixType.INFO, "Game starting in " + config.getInt("auto-start-time") + "!", p);
            }
        }
    }

    /*
     * 
     * ################################################
     * 
     * START GAME
     * 
     * ################################################
     * 
     * 
     */
    public void startGame() {

        if (mode == GameMode.INGAME) {
            return;
        }

        if (activePlayers.size() <= 0) {
            for (Player pl : activePlayers) {
                msgmgr.sendMessage(PrefixType.WARNING, "Not enough players!", pl);
                mode = GameMode.WAITING;
                LobbyManager.getInstance().updateWall(gameID);

            }
            return;
        } else {
            startTime = new Date().getTime();
            startDate = new Date();

            isPvpEnabled = true;
            dmCountdownRunning = false;

            for (Player pl : activePlayers) {

                startingPlayers.add(pl.getName());

                pl.setHealth(pl.getMaxHealth());

                for (PotionEffect effect : pl.getActivePotionEffects()) {
                    pl.removePotionEffect(effect.getType());
                }
                pl.setGameMode(org.bukkit.GameMode.SURVIVAL);

                // clearInv(pl);
                msgmgr.sendFMessage(PrefixType.INFO, "game.goodluck", pl);
            }
            if (config.getBoolean("restock-chest")) {
                SettingsManager.getGameWorld(gameID).setTime(0);
                gcount++;
                tasks.add(Bukkit.getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new NightChecker(), 14400));
            }
            if (config.getInt("grace-period") != 0) {
                for (Player play : activePlayers) {
                    msgmgr.sendMessage(PrefixType.INFO, "You have a " + config.getInt("grace-period") + " second grace period!", play);
                }
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        for (Player play : activePlayers) {
                            msgmgr.sendMessage(PrefixType.INFO, "Grace period has ended!", play);
                        }
                    }
                }, config.getInt("grace-period") * 20);
            }
        }

        mode = GameMode.INGAME;

        // Start scoreboard update thread
        startBoardUpdater();

        startGameTimer();

        LobbyManager.getInstance().updateWall(gameID);
        MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gamestarted", "arena-" + gameID);

    }

    /*
     * 
     * ################################################
     * 
     * COUNTDOWN
     * 
     * ################################################
     * 
     * 
     */
    public int getCountdownTime() {
        return count;
    }

    int count = 20;
    int tid = 0;

    public void countdown(int time) {
        // Bukkit.broadcastMessage(""+time);
        MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gamestarting", "arena-" + gameID, "t-" + time);
        countdownRunning = true;
        count = time;
        Bukkit.getScheduler().cancelTask(tid);

        if (mode == GameMode.WAITING || mode == GameMode.STARTING) {
            mode = GameMode.STARTING;
            tid = Bukkit.getScheduler().scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), new Runnable() {
                @Override
                public void run() {

                    if (count > 0) {
                        if (count % 10 == 0) {
                            msgFall(PrefixType.INFO, "game.countdown", "t-" + count);
                        }
                        if (count == 60) {
                        }
                        if (count < 10) {
                            msgFall(PrefixType.INFO, "game.countdown", "t-" + count);
                            for (Player p : activePlayers) {
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1);

                            }
                        }
                        count--;
                        //LobbyManager.getInstance().updateWall(gameID);
                    } else {

                        startGame();

                        countdownRunning = false;
                        for (Player p : activePlayers) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1.7f);
                            initBoard(p);
                        }

                        Bukkit.getScheduler().cancelTask(tid);

                    }
                }
            }, 0, 20);

        }
    }

    /*
     * 
     * ################################################
     * 
     * REMOVE PLAYER
     * 
     * ################################################
     * 
     * 
     */
    public void removePlayer(final Player p, boolean b) {
        p.teleport(SettingsManager.getInstance().getLobbySpawn());
        /// $("Teleporting to lobby");
        if (mode == GameMode.INGAME) {

            killPlayer(p, b);

        } else {
            sm.removePlayer(p, gameID);
            // if (!b)
            // p.teleport(SettingsManager.getInstance().getLobbySpawn());
            restoreInv(p);
            activePlayers.remove(p);
            inactivePlayers.remove(p);

            for (Object in : spawns.keySet().toArray()) {
                if (spawns.get(in) == p) {
                    spawns.remove(in);
                }
            }

            LobbyManager.getInstance().clearSigns(gameID);
        }

        HookManager.getInstance().runHook("PLAYER_REMOVED", "player-" + p.getName());

        removeScoreboard(p);
        PlayerLeaveArenaEvent pl = new PlayerLeaveArenaEvent(p, this, b);
        Bukkit.getServer().getPluginManager().callEvent(pl);

        LobbyManager.getInstance().updateWall(gameID);
    }

    /*
     * 
     * ################################################
     * 
     * KILL PLAYER
     * 
     * ################################################
     * 
     * 
     */
    public void killPlayer(Player p, boolean left) {
        try {

            clearInv(p);
            if (!left) {
                p.teleport(SettingsManager.getInstance().getLobbySpawn());
            }
            sm.playerDied(p, activePlayers.size(), gameID, new Date().getTime() - startTime);

            if (!activePlayers.contains(p)) {
                return;
            } else {
                restoreInv(p);
            }

            activePlayers.remove(p);
            inactivePlayers.add(p);
            PlayerKilledEvent pk = null;

            if (left) {
                msgFall(PrefixType.INFO, "game.playerleavegame", "player-" + p.getName());
            } else {
                if (mode != GameMode.WAITING && p.getLastDamageCause() != null && p.getLastDamageCause().getCause() != null) {
                    switch (p.getLastDamageCause().getCause()) {
                        case ENTITY_ATTACK:
                            if (p.getLastDamageCause().getEntityType() == EntityType.PLAYER) {
                                Player killer = p.getKiller();

                                msgFall(PrefixType.INFO, "death." + p.getLastDamageCause().getEntityType(),
                                        "player-" + (SurvivalGames.auth.contains(p.getName()) ? ChatColor.DARK_RED + "" + ChatColor.BOLD : "") + p.getName(),
                                        "killer-" + ((killer != null) ? (SurvivalGames.auth.contains(killer.getName()) ? ChatColor.DARK_RED + "" + ChatColor.BOLD : "")
                                                + killer.getName() : "Unknown"),
                                        "item-" + ((killer != null) ? ItemReader.getFriendlyItemName(killer.getItemInHand().getType()) : "Unknown Item"));

                                if (killer != null) {
                                    PointSystem.getQueryHandler().addDeath(p.getName(), gameElapsedSeconds);
                                    PointSystem.getQueryHandler().addKill(killer.getName(), gameElapsedSeconds, 2);

                                    sm.addKill(killer, p, gameID);
                                }

                            } else {
                                msgFall(PrefixType.INFO, "death." + p.getLastDamageCause().getEntityType(), "player-"
                                        + (SurvivalGames.auth.contains(p.getName()) ? ChatColor.DARK_RED + "" + ChatColor.BOLD : "")
                                        + p.getName(), "killer-" + p.getLastDamageCause().getEntityType());
                                PointSystem.getQueryHandler().addDeath(p.getName(), gameElapsedSeconds);

                            }

                            break;
                        default:
                            msgFall(PrefixType.INFO, "death." + p.getLastDamageCause().getCause().name(),
                                    "player-" + (SurvivalGames.auth.contains(p.getName()) ? ChatColor.DARK_RED + "" + ChatColor.BOLD : "") + p.getName(),
                                    "killer-" + p.getLastDamageCause().getCause());
                            PointSystem.getQueryHandler().addDeath(p.getName(), gameElapsedSeconds);

                            break;
                    }
                }
            }

            for (Player pe : activePlayers) {
                Location l = pe.getLocation();
                l.setY(l.getWorld().getMaxHeight());
                l.getWorld().strikeLightningEffect(l);
            }

            if (getActivePlayers() <= config.getInt("endgame.players") && config.getBoolean("endgame.fire-lighting.enabled") && !endgameRunning) {

                tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), new EndgameManager(), 0, config.getInt("endgame.fire-lighting.interval") * 20));
            }

            if (activePlayers.size() < 2 && mode != GameMode.WAITING) {

                Player win = activePlayers.get(0);

                playerWin(p);

                PointSystem.getQueryHandler().addWin(win.getName(), elapsedPointsTime, maximumPointsTime, numStartPlayers, gameElapsedSeconds);
                endDate = new Date();

                PointSystem.getQueryHandler().saveGame(gameID, startingPlayers, win.getName(), endDate, startDate, elapsedPointsTime, maximumPointsTime);

                endGame();
            }

            removeScoreboard(p);

            LobbyManager.getInstance().updateWall(gameID);

        } catch (IllegalArgumentException | IllegalStateException e) {
            SurvivalGames.$("???????????????????????");
            SurvivalGames.$("ID" + gameID);
            SurvivalGames.$(left + "");
            SurvivalGames.$(activePlayers.size() + "");
            SurvivalGames.$(activePlayers.toString());
            SurvivalGames.$(p.getName());
            SurvivalGames.$(p.getLastDamageCause().getCause().name());
        }
    }

    /*
     * 
     * ################################################
     * 
     * PLAYER WIN
     * 
     * ################################################
     * 
     * 
     */
    public void playerWin(Player p) {
        if (GameMode.DISABLED == mode) {
            return;
        }
        Player win = activePlayers.get(0);
        // clearInv(p);
        win.teleport(SettingsManager.getInstance().getLobbySpawn());
        restoreInv(win);
        msgmgr.broadcastFMessage(PrefixType.INFO, "game.playerwin", "arena-" + gameID, "victim-" + p.getName(), "player-" + win.getName());
        LobbyManager.getInstance().display(new String[]{
            win.getName(),
            "",
            "Won the ",
            "Survival Games!"
        }, gameID);

        mode = GameMode.FINISHING;

        clearSpecs();
        win.setHealth(p.getMaxHealth());
        win.setFoodLevel(20);
        win.setFireTicks(0);
        win.setFallDistance(0);

        activePlayers.clear();
        inactivePlayers.clear();
        spawns.clear();

        loadspawns();
        LobbyManager.getInstance().updateWall(gameID);
        MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gameend", "arena-" + gameID);
        removeScoreboard(win);

    }

    public void endGame() {
        mode = GameMode.WAITING;
        resetArena();
        stopBoardUpdater();

        LobbyManager.getInstance().clearSigns(gameID);
        LobbyManager.getInstance().updateWall(gameID);

    }

    /*
     * 
     * ################################################
     * 
     * DISABLE
     * 
     * ################################################
     * 
     * 
     */
    public void disable() {
        disabled = true;
        spawns.clear();

        for (int a = 0; a < activePlayers.size(); a = 0) {
            try {

                Player p = activePlayers.get(a);
                msgmgr.sendMessage(PrefixType.WARNING, "Game disabled!", p);
                removePlayer(p, false);
            } catch (Exception e) {
            }

        }

        for (int a = 0; a < inactivePlayers.size(); a = 0) {
            try {

                Player p = inactivePlayers.remove(a);
                msgmgr.sendMessage(PrefixType.WARNING, "Game disabled!", p);
            } catch (Exception e) {
            }

        }

        clearSpecs();
        queue.clear();

        endGame();
        LobbyManager.getInstance().updateWall(gameID);
        MessageManager.getInstance().broadcastFMessage(PrefixType.INFO, "broadcast.gamedisabled", "arena-" + gameID);

    }

    /*
     * 
     * ################################################
     * 
     * RESET
     * 
     * ################################################
     * 
     * 
     */
    public void resetArena() {

        for (Integer i : tasks) {
            Bukkit.getScheduler().cancelTask(i);
        }

        tasks.clear();
        vote = 0;
        voted.clear();

        mode = GameMode.RESETING;
        endgameRunning = false;

        isPvpEnabled = true;
        dmCountdownRunning = false;
        inDeathmatch = false;

        startingPlayers.clear();
        Bukkit.getScheduler().cancelTask(countdownTaskID);
        gameLengthSeconds = config.getInt("deathmatch.time") * 60;

        Bukkit.getScheduler().cancelTask(endgameTaskID);
        GameManager.getInstance().gameEndCallBack(gameID);
        QueueManager.getInstance().rollback(gameID, false);
        LobbyManager.getInstance().updateWall(gameID);

    }

    public void resetCallback() {
        if (!disabled) {
            enable();
        } else {
            mode = GameMode.DISABLED;
        }
        LobbyManager.getInstance().updateWall(gameID);
    }

    public void saveInv(Player p) {
        ItemStack[][] store = new ItemStack[2][1];

        store[0] = p.getInventory().getContents();
        store[1] = p.getInventory().getArmorContents();

        inv_store.put(p, store);

    }

    public void restoreInvOffline(String p) {
        restoreInv(Bukkit.getPlayer(p));
    }

    /*
     * 
     * ################################################
     * 
     * SPECTATOR
     * 
     * ################################################
     * 
     * 
     */
    public void addSpectator(Player p) {

        if (mode != GameMode.INGAME) {
            msgmgr.sendMessage(PrefixType.WARNING, "You can only spectate running games!", p);
            return;
        }

        clearInv(p);
        p.teleport(SettingsManager.getInstance().getSpawnPoint(gameID, 1).add(0, 10, 0));

        HookManager.getInstance().runHook("PLAYER_SPECTATE", "player-" + p.getName());

        p.setGameMode(org.bukkit.GameMode.SPECTATOR);

        spectators.add(p.getName());
        msgmgr.sendMessage(PrefixType.INFO, "You are now spectating! Use /sg spectate again to return to the lobby.", p);
        msgmgr.sendMessage(PrefixType.INFO, "Left click to teleport to the in-game players.", p);
        nextspec.put(p, 0);

        initBoard(p);
    }

    public void removeSpectator(Player p) {
        ArrayList<Player> players = new ArrayList<Player>();
        players.addAll(activePlayers);
        players.addAll(inactivePlayers);
        p.setGameMode(org.bukkit.GameMode.SURVIVAL);
        p.setFallDistance(0);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.teleport(SettingsManager.getInstance().getLobbySpawn());
        removeScoreboard(p);

        spectators.remove(p.getName());

        nextspec.remove(p);
    }

    public void clearSpecs() {

        for (int a = 0; a < spectators.size(); a = 0) {
            removeSpectator(Bukkit.getPlayerExact(spectators.get(0)));
        }
        spectators.clear();
        nextspec.clear();
    }

    public int getSpectatingPlayers() {
        return spectators.size();
    }

    public HashMap<Player, Integer> getNextSpec() {
        return nextspec;
    }

    @SuppressWarnings("deprecation")
    public void restoreInv(Player p) {
        try {
            clearInv(p);
            p.getInventory().setContents(inv_store.get(p)[0]);
            p.getInventory().setArmorContents(inv_store.get(p)[1]);
            inv_store.remove(p);
            p.updateInventory();
        } catch (Exception e) { /*
             * p.sendMessage(ChatColor.RED+
             * "Inentory failed to restore or nothing was in it."
             * );
             */

        }
    }

    @SuppressWarnings("deprecation")
    public void clearInv(Player p) {
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < inv.length; i++) {
            inv[i] = null;
        }
        p.getInventory().setContents(inv);
        inv = p.getInventory().getArmorContents();
        for (int i = 0; i < inv.length; i++) {
            inv[i] = null;
        }
        p.getInventory().setArmorContents(inv);
        p.updateInventory();

    }

    class NightChecker implements Runnable {

        boolean reset = false;
        int tgc = gcount;

        @Override
        public void run() {
            if (SettingsManager.getGameWorld(gameID).getTime() > 14000) {
                for (Player pl : activePlayers) {
                    msgmgr.sendMessage(PrefixType.INFO, "Chests restocked!", pl);
                }
                GameManager.openedChest.get(gameID).clear();
                reset = true;
            }

        }
    }

    class EndgameManager implements Runnable {

        @Override
        public void run() {
            for (Player player : activePlayers.toArray(new Player[0])) {
                Location l = player.getLocation();
                l.add(0, 5, 0);
                player.getWorld().strikeLightningEffect(l);

                boolean hascomp = player.getInventory().contains(Material.COMPASS);

                if ((!hascomp)) {
                    player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.COMPASS, 1)});
                    msgmgr.sendMessage(PrefixType.INFO, "You have been given a compass for EndGame! Use it to track players around you!", player);

                }
            }
        }
    }

    /**
     *
     *
     *
     * GAME TIMER UNTIL DEATHMATCH
     *
     *
     *
     */
    public void startGameTimer() {

        countdownTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {

                gameElapsedSeconds++;

                if (elapsedPointsTime < maximumPointsTime) {
                    elapsedPointsTime++;
                }

                if (deathmatchEnabled) {
                    gameLengthSeconds--;
                }

                if (gameLengthSeconds >= 14) {
                    if (gameLengthSeconds == 320) {
                        for (Player play : activePlayers) {
                            if (play.isOnline()) {
                                msgmgr.sendMessage(PrefixType.INFO, "The deathmatch will start in 5 minutes!", play);
                                play.playSound(play.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 10, 1);
                            }
                        }
                    }

                    if (gameLengthSeconds == 80) {
                        for (Player play : activePlayers) {
                            if (play.isOnline()) {
                                msgmgr.sendMessage(PrefixType.INFO, "The deathmatch will start in 1 minute!", play);
                                play.playSound(play.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 10, 1);

                            }
                        }
                    }

                    if (gameLengthSeconds <= 30 && gameLengthSeconds >= 21) {

                        for (Player play : activePlayers) {
                            if (play.isOnline()) {
                                int ngameLengthSeconds = gameLengthSeconds - 20;
                                if (gameLengthSeconds == 30) {

                                    msgmgr.sendMessage(PrefixType.INFO, "Teleportation to cornucopia for deathmatch in " + ngameLengthSeconds + " ...", play);
                                    play.playSound(play.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);
                                } else {
                                    msgmgr.sendMessage(PrefixType.INFO, ngameLengthSeconds + " ...", play);
                                    play.playSound(play.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);
                                }
                            }
                        }
                    }

                    if (gameLengthSeconds == 20) {

                        isPvpEnabled = false;
                        dmCountdownRunning = true;

                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {

                                    p.teleport(SettingsManager.getInstance().getSpawnPoint(gameID, a));
                                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 18000, 3));
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);
                                    break;
                                }
                            }
                        }

                    }
                    if (gameLengthSeconds == 19) {

                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {

                                    msgmgr.sendMessage(PrefixType.INFO, "Teleported! Deathmatch starting in 5...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);
                                    break;
                                }
                            }
                        }
                    }
                    if (gameLengthSeconds == 18) {
                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {
                                    msgmgr.sendMessage(PrefixType.INFO, "4 ...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);
                                    break;
                                }
                            }
                        }

                    }
                    if (gameLengthSeconds == 17) {
                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {
                                    msgmgr.sendMessage(PrefixType.INFO, "3 ...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);

                                }
                            }
                        }

                    }

                    if (gameLengthSeconds == 16) {
                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {
                                    msgmgr.sendMessage(PrefixType.INFO, "2 ...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);

                                }
                            }
                        }

                    }

                    if (gameLengthSeconds == 15) {
                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {
                                    msgmgr.sendMessage(PrefixType.INFO, "1 ...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);

                                }
                            }
                        }

                    }

                    if (gameLengthSeconds == 15) {
                        for (Player p : activePlayers) {
                            for (int a = 0; a < spawns.size(); a++) {
                                if (spawns.get(a) == p) {
                                    msgmgr.sendMessage(PrefixType.INFO, "0 ...", p);

                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);

                                }
                            }
                        }

                    }

                    if (gameLengthSeconds <= 5) {
                        for (Player play : activePlayers) {
                            if (play.isOnline()) {
                                msgmgr.sendMessage(PrefixType.INFO, "Deathmatch starting in " + gameLengthSeconds + " ...", play);
                                play.playSound(play.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1f);

                            }
                        }
                    }

                } else {
                    if (mode == GameMode.INGAME) {

                        //calculate borders based on first spawnpoint
                        Integer borderSize = config.getInt("deathmatch.border.radius");

                        Integer spawns = SettingsManager.getInstance().getSpawnCount(gameID);

                        double totalX = 0, totalZ = 0;

                        for (int i = 1; i <= spawns; i++) {

                            totalX += SettingsManager.getInstance().getSpawnPoint(gameID, i).getX();
                            totalZ += SettingsManager.getInstance().getSpawnPoint(gameID, i).getZ();

                        }

                        //calculate average added / points
                        double centerX = totalX / spawns;
                        double centerZ = totalZ / spawns;

                        debug("The calculated center is: X: " + centerX + " Z: " + centerZ + "");
                        debug("Total number of spawns in arena " + gameID + " : " + spawns);
                        Location center = new Location(SettingsManager.getGameWorld(gameID), centerX, 0.0, centerZ);
                        Location center2 = new Location(SettingsManager.getGameWorld(gameID), centerX, 0.0, centerZ);

                        //add the 1337 values to the center vectors to get the border points p1 and p2
                        Location p1 = center.add(-borderSize, 0, -borderSize);
                        Location p2 = center2.add(borderSize, 0, borderSize);

                        try {
                            addBorder(p1, p2);
                            debug("Borders: P1: " + p1.toString() + " P2: " + p2.toString());
                        } catch (Exception ex) {

                        }

                        for (Player play : activePlayers) {
                            if (play.isOnline()) {
                                msgmgr.sendMessage(PrefixType.INFO, "Fight to the death!!!", play);
                                play.playSound(play.getLocation(), Sound.BLOCK_NOTE_HARP, 10, 1.6f);

                                if (play.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                                    play.removePotionEffect(PotionEffectType.BLINDNESS);
                                }

                            }
                        }

                        isPvpEnabled = true;
                        dmCountdownRunning = false;
                        inDeathmatch = true;
                    }

                    Bukkit.getScheduler().cancelTask(countdownTaskID);

                    gameLengthSeconds = 0;
                }
            }
        }, 0, 20);

        if (Bukkit.getScheduler().isCurrentlyRunning(countdownTaskID)) {
            tasks.add(countdownTaskID);
        }

    }

    /**
     *
     * DEATHMATCH BORDER
     *
     */
    public HashMap<String, Location> getBorders() {
        return borders;
    }

    public boolean isBorderActive() {
        return isBorderActive;
    }

    public boolean isDeathmatch() {
        return inDeathmatch;
    }

    public Location getRectPoint1() {
        return rectPoint1;
    }

    public Location getRectPoint2() {
        return rectPoint2;
    }

    public void addBorder(Location p1, Location p2) throws Exception {
        rectPoint1 = p1;
        rectPoint2 = p2;

        // new border is active by default
        isBorderActive = true;

        if (rectPoint1.getWorld().equals(rectPoint2.getWorld())) {

            borders.put("p1", rectPoint1);
            borders.put("p2", rectPoint2);

        } else {
            throw new Exception("Border points are at different worlds.");
        }
    }

    public Double[] checkBorder(Location location, HashMap<String, Location> points, double buffer) {
        // New x and z: null by default
        Double[] newXZ = {null, null};
        Location p1 = points.get("p1");
        Location p2 = points.get("p2");

        // check if player is withing the X borders
        newXZ[0] = _checkBorder(location.getX(), p1.getX(), p2.getX(), buffer);
        // check if player is withing the Z borders
        newXZ[1] = _checkBorder(location.getZ(), p1.getZ(), p2.getZ(), buffer);

        // Do nothing, if no new coordinates have been calculated.
        if (newXZ[0] == null && newXZ[1] == null) {
            return null;
        }
        return newXZ;
    }

    public Double _checkBorder(double location, double border1, double border2, double buffer) {
        double bigBorder = Math.max(border1, border2);
        double smallBorder = Math.min(border1, border2);

        // if location is between borders do nothing
        if (location >= smallBorder && location <= bigBorder) {
            return null;
        } else {
            if (location > bigBorder) {
                // if location is outside of the bigBorder, teleport to the bigBorder
                return bigBorder - buffer;
            } else {
                // if location is outside of the smallBorder, teleport to the smallBorder
                return smallBorder + buffer;
            }
        }
    }

    public Double goUpUntilFreeSpot(Location newLocation) {
        // go up in height until the player can stand in AIR
        Block footBlock = newLocation.getBlock();
        Block headBlock = newLocation.getBlock().getRelative(BlockFace.UP);
        while (footBlock.getType() != Material.AIR || headBlock.getType() != Material.AIR) {
            byte offset = 1;
            if (headBlock.getType() != Material.AIR) {
                offset = 2;
            }
            footBlock = footBlock.getRelative(0, offset, 0);
            headBlock = headBlock.getRelative(0, offset, 0);
        }
        // set the y value to a spot where the player can stand free
        return (double) footBlock.getY();
    }

    public boolean isBlockInArena(Location v) {
        return arena.containsBlock(v);
    }

    public boolean isProtectionOn() {
        long t = startTime / 1000;
        long l = config.getLong("grace-period");
        long d = new Date().getTime() / 1000;
        if ((d - t) < l) {
            return true;
        }
        return false;
    }

    public int getID() {
        return gameID;
    }

    public int getActivePlayers() {
        return activePlayers.size();
    }

    public int getInactivePlayers() {
        return inactivePlayers.size();
    }

    public Player[][] getPlayers() {
        return new Player[][]{
            activePlayers.toArray(new Player[0]),
            inactivePlayers.toArray(new Player[0])
        };
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> all = new ArrayList<Player>();
        all.addAll(activePlayers);
        all.addAll(inactivePlayers);
        return all;
    }

    public boolean inDeathmatchCountdown() {
        return dmCountdownRunning;
    }

    public boolean isSpectator(Player p) {
        return spectators.contains(p.getName());
    }

    public boolean isInQueue(Player p) {
        return queue.contains(p);
    }

    public boolean isPlayerActive(Player player) {
        return activePlayers.contains(player);
    }

    public boolean isPlayerinactive(Player player) {
        return inactivePlayers.contains(player);
    }

    public boolean hasPlayer(Player p) {
        return activePlayers.contains(p) || inactivePlayers.contains(p);
    }

    public GameMode getMode() {
        return mode;
    }

    public synchronized void setRBPercent(double d) {
        rbpercent = d;
    }

    public double getRBPercent() {
        return rbpercent;
    }

    public void setRBStatus(String s) {
        rbstatus = s;
    }

    public String getRBStatus() {
        return rbstatus;
    }

    public String getName() {
        return "Arena " + gameID;
    }

    public void msgFall(PrefixType type, String msg, String... vars) {
        for (Player p : getAllPlayers()) {
            msgmgr.sendFMessage(type, msg, p, vars);
        }
    }

    public Boolean pvpEnabled() {
        return isPvpEnabled;
    }
}
