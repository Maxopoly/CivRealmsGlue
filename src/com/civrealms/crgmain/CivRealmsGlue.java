package com.civrealms.crgmain;

import com.civrealms.crgpve.SekkritClass;
import com.civrealms.delayedtasks.DelayedAsync.BeaconDisable;
import com.civrealms.delayedtasks.DelayedAsync.SaveFingerPrintDB;
import com.civrealms.delayedtasks.DelayedAsync.SlowPlayerLoopTask;
import com.civrealms.delayedtasks.DelayedSync.AntiBoatSinkCheck;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.programmerdan.minecraft.banstick.BanStick;
import com.programmerdan.minecraft.banstick.data.BSPlayer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Logger;
import net.minelink.ctplus.CombatTagPlus;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;

public class CivRealmsGlue extends JavaPlugin implements PluginMessageListener {
    //Other classes/meta plugin stuff
    private GroupManager gm = NameAPI.getGroupManager();
    CombatTagPlus cpt = null; 
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("US/Pacific"));
    public static Logger LOG = Logger.getLogger("CivRealmsGlue");
    public ProtocolManager pmgr;
    //###########
    public static String world;
    public String rootBoxDirectory;
    
    //storage
	public HashMap<String,Long>firstPlayed = new HashMap<String,Long>();
    private HashMap<UUID,Integer> slowLoopKicks = new HashMap<UUID,Integer>(); //person denied, list of people who have denied them.
    private static HashSet<UUID> usedTheirTeleport = new HashSet<>();
    public HashMap<String,Location> ottTeleportMemory = new HashMap<String,Location>();
    private HashSet<UUID> noAutoReplant = new HashSet<>();
    private HashMap<UUID,Integer> newFriendChat = new HashMap<UUID,Integer>();
    public HashMap<String,Double> avgProxies = new HashMap<String,Double>();
    private ArrayList<Entity> entitiesForFastLoop = new ArrayList<Entity>();
    public HashMap<UUID,String> fingerprintMap = new HashMap<UUID,String>();
    public HashMap<String,UUID> fingerprintMapReverse = new HashMap<String,UUID>(); 
    public HashSet<String> whitelistedConnections = new HashSet<String>();
    
    //Lazy man's "configs"
    public int noTicks = 10;
    public double hitVertical = 0.5;  //how much to reduce vertical bounce on a hit (to counteract knockback)
    public double awayFromYouDampener = 0.25; //positive means dampening here, smaller number is less dampening
    public double knockBackEnchantMultiplierVertical = 5.0; //multiplies the amount ADDED to vertical. So 0 here would undo the vertical normal dampening (leaving behind vanilla knockback enchant), negative would make it MORE effective at vertical, etc.
    public double knockBackEnchantMultiplierAway = -1.0; //multiplies the amount of dampening ADDED, so 0 would undo normal dampening, 2.0 would help counteract the extra enchant effect, negative numbers would ENCHANCE vanilla knockback enchant.
    public boolean woodTestingOn = false;
    public int slowLoopTicks = 200;
    private long lastSlowLoop = System.currentTimeMillis();
    public int noIPGracePeriod = 8000; //milliseconds a person has before lack of registered IP data boots them. default 2 mins.
    public final double MinecraftServersOrg = 0.75;
    public final double PlanetMinecraftCom = 1.0;
    public final double MCSL = 0.75;
    public double swimLimit = 4.0; // units are full itemstacks, so 64 dirt = 1.0, one armor piece = 1.0, 8 pearls = 0.5, etc.
	public double chickenFeatherReplace = 0.08; // chance for a vanilla chicken egg to drop instead of a feather
	public double stickDropChance = 0.05; // chance for decaying leaves to drop a stick
	public double appleDropChance = 0.0015; // 0.005 is vanilla. Chance for (only oak and dark oak) decaying leaves to drop an apple
    public double saplingDropChance = 0.05;
	public int miningCeilLevel = 254; // where you will get bounced back in the mining realm
	public int sportsHeight = 41; // distance above iron blocks that sports balls can be moved
	public double goldDuraHitChance = 0.0625; // chance that gold tool will take a hit on use, so bronze is 1/this times stronger than vanilla gold
	public double ironSteelDuraHitChance = 0.8; // applies to both iron and steel, but steel also have dura enchants (x2 so triple iron after this <--- buff)
	public int pistonBedrockExclusion = 3; // distance from bedrock you can't use pistons within to prevent glitching through bedrock
    public double normalFingerprintChance = 0.005;
    public double shieldedFingerprintChance = 0.00003;
    public double manualBreakStickDropChance = 0.25;
    public double fistLogBreakChance = 0.05;
	public double stoneStrength = 4; // 1/(10-this) chance to fail on turning stone to cobble
	public double stonePickCobbleFailChance = 0.33; // chance that stone picks don't work on cobble
	public double bronzeStrength = 5.5; // see above
	public double ironStrength = 6; // see above
	public double steelStrength = 8.5; // see above
	public double caveinChance = 0.005; // chance of a cavein per stone or cobble dug
	public double caveinRadius = 7.5; // how far out to affect in a circle
	public int caveinDown = -3; // how far down to affect as a negative number
	public int caveinUp = 6; // how far up to affect
	public double scorpionChance = .015; // chance of a scorpion per sand dug in relevant biomes
    public double scarabChance = 0.0032; //0.003
	public double cobbleContagionChance = 0.33; // chance of neighboring stone on mining or cavein to become cobble
    public int perWorldAltThreshold = 80;
    public int lobbyNorthSafeBoundary = -9;
    public int lobbySouthSafeBoundary = 19;
    public int lobbyEastSafeBoundary = -22;
    public int lobbyWestSafeBoundary = -49;
    public int lobbyCenterX = -36;
    public int lobbyCenterZ = 5;
    public int lobbyCenterY = 44;
    public double chorusFruitChance = 0.09; // chance of a chorus fruit per cactus farmed.
    public double antiInfinityCactusChance = 0.33; // a small chance to not get any drops after the chorus chance to just stop infinite refarming
    
    //db creds
    private static Connection connection;
    private String host;
    private String database;
    private String username;
    private String password;
    private int port;
    
    //getters
    public HashSet<UUID> getUsedTheirTeleport(){return usedTheirTeleport;}
    public HashSet<UUID> getNoAutoReplant(){return noAutoReplant;}
    public CombatTagPlus getCPT(){return cpt;}

	// Fired when plugin is first enabled
	@Override
	public void onEnable() {
        this.world = getConfig().getString("worldName");
        getLogger().info("DEBUG " + getDataFolder());
        pmgr = ProtocolLibrary.getProtocolManager();
        cpt = (CombatTagPlus)(getServer().getPluginManager().getPlugin("CombatTagPlus"));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.ContrabandListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.PVEMiscellaneousListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpvp.PVPListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.BlockBreakListenerNonCrops(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.CustomMobDrops(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgcrops.BlockBreakListenerCrops(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.LagMonitoringListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.AltAccountListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.SekkritClass(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.FingerprintListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.AdminToolsListener(this), this);
		getServer().getPluginManager().registerEvents(new com.civrealms.crgmain.LobbyManager(this), this);
        getServer().getPluginManager().registerEvents(new com.civrealms.crgpve.TreeFelling(this), this);
        getServer().getPluginManager().registerEvents(new com.civrealms.mobcontrol.MobListener(this), this);
		this.getCommand("chisel").setExecutor(new CommandHandler(this));
        this.getCommand("ott").setExecutor(new CommandHandler(this));
        this.getCommand("ottinvite").setExecutor(new CommandHandler(this));
		this.getCommand("ottuninvite").setExecutor(new CommandHandler(this));
        this.getCommand("togglereplant").setExecutor(new CommandHandler(this));
        this.getCommand("compareprints").setExecutor(new CommandHandler(this));
        this.getCommand("createprints").setExecutor(new CommandHandler(this));
        this.getCommand("lookupprints").setExecutor(new CommandHandler(this));
        this.getCommand("PVPsetKBV").setExecutor(new CommandHandler(this));
        this.getCommand("PVPsetKBA").setExecutor(new CommandHandler(this));
        this.getCommand("PVPsetRV").setExecutor(new CommandHandler(this));
        this.getCommand("PVPsetRA").setExecutor(new CommandHandler(this));
        this.getCommand("PVPsetNoTicks").setExecutor(new CommandHandler(this));
        this.getCommand("PVPtoggleWoodTesting").setExecutor(new CommandHandler(this));
        this.getCommand("inventorydl").setExecutor(new CommandHandler(this));
        this.getCommand("ispbl").setExecutor(new CommandHandler(this));
        this.getCommand("ispwl").setExecutor(new CommandHandler(this));
        this.getCommand("removethorns").setExecutor(new CommandHandler(this));
        this.getCommand("vote").setExecutor(new CommandHandler(this));
        this.getCommand("main").setExecutor(new CommandHandler(this));
        this.getCommand("slowloop").setExecutor(new CommandHandler(this));
        this.getCommand("noIPGracePeriod").setExecutor(new CommandHandler(this));
        this.getCommand("dust").setExecutor(new CommandHandler(this));
        
        try (ObjectInputStream oi = new ObjectInputStream(new FileInputStream("./plugins/CivRealmsGlue/usedTheirTeleport.ser"))){
            Object obj = oi.readObject();
            this.usedTheirTeleport = (HashSet<UUID>)obj;
        } catch (Exception e){LOG.info("Loading usedTheirTeleport hash failed!");}
        
        try (ObjectInputStream oi = new ObjectInputStream(new FileInputStream("./plugins/CivRealmsGlue/noAutoReplant.ser"))){
            Object obj = oi.readObject();
            this.noAutoReplant = (HashSet<UUID>)obj;
        } catch (Exception e){LOG.info("Loading noAutoReplant hash failed!");}

		this.saveDefaultConfig();
        
        //init DB connection
        this.host = this.getConfig().getString("hostname");
        this.port = this.getConfig().getInt("port");
        this.database = this.getConfig().getString("database");
        this.username = this.getConfig().getString("username");
        this.password = this.getConfig().getString("password");
        this.rootBoxDirectory = this.getConfig().getString("rootDir");
        
        //load up DBs
        loadFingerPrintDB();
        initOTTDB();
        initEnvironmentalFingerprintDB();
        loadAvgProxies();
        clearOldFingerprints();
        initISPWhitelist();
       
        //LOOP TASKS
        
        //these shenanigans are so that we can change the scheduling mid server. Slower = more forgiving to newfriends and TPS, faster = more aggressive against VPNers
        //By scheduling a task that schedules another task, the timing can be adjusted outside of bukkit's scheduling black box
        BukkitRunnable r = new BukkitRunnable() { 
            public void run() {
                if (System.currentTimeMillis() - lastSlowLoop > slowLoopTicks*50){
                    BukkitTask slow = (new SlowPlayerLoopTask(CivRealmsGlue.this)).runTaskAsynchronously(CivRealmsGlue.this);
                    lastSlowLoop = System.currentTimeMillis();
                }
            }
        };
        r.runTaskTimerAsynchronously(this,400,20);
        
        //These loops don't have any dynamic parameters:
        new BeaconDisable(this).runTaskTimerAsynchronously(this,200,7); //10 second delay after restart, then once every third of a second
        new AntiBoatSinkCheck(this).runTaskTimer(this,200,60); //10 second delay after restart, then once every 3 seconds
	}

	// Fired when plugin is disabled
	@Override
	public void onDisable() {
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream("./plugins/CivRealmsGlue/usedTheirTeleport.ser"))){
            oo.writeObject(this.usedTheirTeleport);
        } catch (Exception e){LOG.info("Saving usedTheirTeleport hash failed!");}
        
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream("./plugins/CivRealmsGlue/noAutoReplant.ser"))){
            oo.writeObject(this.noAutoReplant);
        } catch (Exception e){LOG.info("Saving noAutoReplant hash failed!");}

        //save fingerprints from DB
        SaveFingerPrintDB sfpdb = new SaveFingerPrintDB(this, getConnection());
        sfpdb.runTaskAsynchronously(this);
        
        SekkritClass.flushSpecialLogs();
	}
    
    //This is a generic method for any/all bungee messages handled in the plugin (only one for now)
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
          return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("ott")) {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            try {
              String display_name_invitee = msgin.readUTF();
              int x = msgin.readInt();
              int y = msgin.readInt();
              int z = msgin.readInt();
              Location loc = new Location(Bukkit.getWorld(this.getConfig().getString("worldName")),x,y,z);
              ottTeleportMemory.put(display_name_invitee, loc);
            }
            catch (IOException e) {
              e.printStackTrace();
            }
            return;
        } 
    }
    
    //fingerprint methods
    public void clearOldFingerprints(){
        try {
            openConnection();
            Statement statement = getConnection().createStatement();
            statement.executeUpdate("delete from fingerprints_experimental where timestamp < " + (System.currentTimeMillis()-604800000) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void loadFingerPrintDB(){
        try{
            openConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table if not exists `fingerprints` (`uuid` varchar(36),`fingerprint` varchar(8), PRIMARY KEY (`uuid`));");
            
            ResultSet result = statement.executeQuery("select * from fingerprints;");
            while (result.next()) {
                String uuid = result.getString(1);
                String fingerprint = result.getString(2);
                fingerprintMap.put(UUID.fromString(uuid), fingerprint);
                fingerprintMapReverse.put(fingerprint,UUID.fromString(uuid));
            }
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public void initEnvironmentalFingerprintDB(){
        try{
            openConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table if not exists `fingerprints_environmental` (`uuid` VARCHAR(36), `x` int, `y` int, `z` int, `world` VARCHAR(36), `timestamp` bigint, `fingerprint` varchar(8), PRIMARY KEY (`uuid`));");
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public String getFingerPrintCode(UUID uid){
        //for now, the 8 digit code is useless, basically, but it's there to support future features retroactively if/when fingerprints can be found places. It would ONLY be the codes, then, not the names.
        if (!fingerprintMap.containsKey(uid)){
            // HEX: 0-9, a-f. For example: 6587fddb, c0f182c1
            String printID = RandomStringUtils.random(8, "0123456789abcdef");
            while (fingerprintMap.containsValue(printID)){
                printID = RandomStringUtils.random(8, "0123456789abcdef");
            }
            fingerprintMap.put(uid, printID);
            fingerprintMapReverse.put(printID,uid);
            SaveFingerPrintDB sfpdb = new SaveFingerPrintDB(this, getConnection());
            sfpdb.runTaskAsynchronously(this);
            return printID;
        } else {
            return fingerprintMap.get(uid);
        }
    }
    
    //ott methods
    public void initOTTDB(){
        try{
            openConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table if not exists `ott_invites` (`display_name_inviter` varchar(36), `display_name_invitee` varchar(36), `timestamp` bigint, `x` int, `y` int, `z` int, `world` VARCHAR(36), PRIMARY KEY (`display_name_inviter`,`display_name_invitee`));");
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public void setPostOTTInventory(Player player){
        //I decided I don't give a shit if people dupe a few bronze picks and starting books
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.addItem(new ItemStack(Material.FISHING_ROD, 1));
        inv.addItem(new ItemStack(Material.BREAD, 16));
        inv.addItem(new ItemStack(Material.BED, 1));
        
        //pick
        ItemStack oldBronzePick = new ItemStack(Material.GOLD_PICKAXE, 1);
        ItemMeta pickmeta = oldBronzePick.getItemMeta();
        pickmeta.setDisplayName("Bronze Pickaxe");
        String la[] = new String[] { "Made of cast and hammered bronze","Mining Mode (/chisel to toggle)" };
        List<String> lorearray = Arrays.asList(la);
        pickmeta.setLore(lorearray);
        oldBronzePick.setItemMeta(pickmeta);
        inv.addItem(oldBronzePick);
        
        //claim bastion
        ItemStack claimBastion = new ItemStack(Material.ENDER_CHEST, 1);
        ItemMeta bastionmeta = claimBastion.getItemMeta();
        bastionmeta.setDisplayName("Claim Bastion");
        la = new String[] { "Claim Bastions prevent unfriendly block placements and allow easier building. 21x21 area. Place at bedrock. Reinforce (/ctf) to activate." };
        lorearray = Arrays.asList(la);
        bastionmeta.setLore(lorearray);
        claimBastion.setItemMeta(bastionmeta);
        inv.addItem(claimBastion);
        
        //book
        String worldName = this.getConfig().getString("worldName");
        if ((worldName.contentEquals("world") || worldName.contentEquals("aqua")) && player.getWorld().getBlockAt(-3288, 1, -4368).getType().equals(Material.CHEST)) {
            inv.addItem(((Chest) (player.getWorld().getBlockAt(-3288, 1, -4368).getState())).getBlockInventory().getItem(0));
        } else if (worldName.contentEquals("new_world") && player.getWorld().getBlockAt(-3288, 1, -4368).getType().equals(Material.CHEST)){
            inv.addItem(((Chest) (player.getWorld().getBlockAt(-3288, 1, -4368).getState())).getBlockInventory().getItem(0));
        } else if (worldName.contentEquals("CivRealms_taiga_island") && player.getWorld().getBlockAt(-3288, 1, -4368).getType().equals(Material.CHEST)){
            inv.addItem(((Chest) (player.getWorld().getBlockAt(-3288, 1, -4368).getState())).getBlockInventory().getItem(0));
        }
        
        //clear out ott entry
        ottTeleportMemory.remove(player.getDisplayName());
    }
    
    //ISP "whitelist" feature
    
    public void initISPWhitelist(){
        try{
            openConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table if not exists `isp_whitelist` (`connection` varchar(48), PRIMARY KEY (`connection`));");
            ResultSet rs = statement.executeQuery("select * from isp_whitelist;");
            while (rs.next()){
                whitelistedConnections.add(rs.getString(1).toLowerCase());
            }
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public boolean checkISPAsWL(String connection){
        if(whitelistedConnections.contains(connection.toLowerCase())){
            return true;
        } else {
            return false;
        }
    }
    
    //misc/util methods:
    public String getHumanTimestamp() {//as of now
        calendar.setTimeInMillis(System.currentTimeMillis());
        return (calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.DAY_OF_MONTH) +
                  "/" + calendar.get(Calendar.YEAR) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) +
                  ":" + calendar.get(Calendar.SECOND));
     }
    
    public boolean checkInventory(Player sender, Material typeToSearchFor){
        if (sender.getInventory().contains(typeToSearchFor)){return true;}
        return false;
    }
    
    public ArrayList<Entity> getEntities(World w) { // to reduce chance of fastbreak concurrentmodification, but not the right way to do it
        return (ArrayList<Entity>)(new ArrayList(w.getEntities()).clone());
    }
    
    public boolean compareAlts(UUID uuidA, UUID uuidB){
        Set<BSPlayer> directAssoc = BSPlayer.byUUID(uuidA).getTransitiveSharedPlayers(true);
        for (BSPlayer alt : directAssoc){
            LOG.info("alt " + alt.getUUID() + " originalA " + uuidA + " originalB " + uuidB);
            if (alt.getUUID().equals(uuidB)){
                return true;
            }
        }
        return false;
    }
    
    private void loadAvgProxies(){
        try{
            openConnection();
            Statement statement = getConnection().createStatement();
                    ResultSet rs = statement.executeQuery("select distinct connection, avg(proxy) FROM bs_ip_data group by 1");
            while (rs.next()){
                avgProxies.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        //check blacklist
        Set<String> connectionSet = avgProxies.keySet();
        ConfigurationSection overrideISPs = BanStick.getPlugin().getConfig().getConfigurationSection("overrideISPs");
        Set<String> overrideSet = overrideISPs.getKeys(false);
        for (String isp : connectionSet){
            for (String override : overrideSet) {
                if (isp != null){
                    if (isp.contains(override.replace("^"," "))){
                        avgProxies.put(isp, (Double)(overrideISPs.get(override)));
                        LOG.info("override isp " + isp + " value " + (Double)(overrideISPs.get(override)));
                    }
                }
            }
        }
    }
    
    public void updateSlowLoopKicks(Player p, int penalty){
        if (slowLoopKicks.containsKey(p.getUniqueId())){
            slowLoopKicks.put(p.getUniqueId(), slowLoopKicks.get(p.getUniqueId()) + penalty);
        } else {
            slowLoopKicks.put(p.getUniqueId(),penalty);
        }
        if(slowLoopKicks.get(p.getUniqueId()) >= 10){
            Bukkit.getScheduler().runTask(this, new Runnable() {
                public void run() {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    String command = "banstick " + p.getDisplayName() + " Autobanned for being kicked too many times in one day for various infractions like ban evasion, pearl evasion, alts at high load, VPNs, etc. Contact reddit.com/r/civrealms 'message the moderators' to sort the issue out.";
                    Bukkit.dispatchCommand(console, command);
                    LOG.info("[CivRealmsGlue]: Banned a player, " + p.getDisplayName() + " for being kicked too many times in a day for other things.");
                }
            });
        }
    }
    
    public void openConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username, this.password);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Connection getConnection(){
        return connection;
    }
}