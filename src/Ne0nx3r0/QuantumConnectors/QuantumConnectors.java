package Ne0nx3r0.QuantumConnectors;

import Ne0nx3r0.QuantumConnectors.Listeners.*;
import Ne0nx3r0.QuantumConnectors.Circuit.*;
import java.io.File;
import org.bukkit.util.config.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.List;

public class QuantumConnectors extends JavaPlugin {
    private final QuantumConnectorsBlockListener blockListener = new QuantumConnectorsBlockListener(this);
    private final QuantumConnectorsPlayerListener playerListener = new QuantumConnectorsPlayerListener(this);
    private final QuantumConnectorsWorldListener worldListener = new QuantumConnectorsWorldListener(this);

    public static Map<String,Integer> circuitTypes = new HashMap<String,Integer>();

    public static CircuitManager circuits;

    private static int AUTO_SAVE_ID = -1;

    public final int typeQuantum = 0;
    public final int typeOn = 1;
    public final int typeOff = 2;
    public final int typeToggle = 3;
    public final int typeReverse = 4;
    public final int typeRandom = 5;

//Configurables
    private int MAX_CHAIN_LINKS = 3;
    private int AUTOSAVE_INTERVAL = 10;//specified here in minutes
    private int CHUNK_UNLOAD_RANGE = 0; //number of chunks surrounding the circuit to keep around when unloading chunks
    private List<String> DISSABLED_WORLDS; // Want to dissable some worlds? Comming soon!

    // Public "gets"
    public int getChain() {
        return MAX_CHAIN_LINKS;
    }
    public int getChunkUnloadRange() {
        return CHUNK_UNLOAD_RANGE;
    }
    
    public void onEnable(){
//Register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Normal, this);
        
//Setup circuits
        circuitTypes.put("quantum",typeQuantum);
        circuitTypes.put("on",typeOn);
        circuitTypes.put("off",typeOff);
        circuitTypes.put("toggle",typeToggle);
        circuitTypes.put("reverse",typeReverse);
        circuitTypes.put("random",typeRandom);

//Configuration
        setupConfig();

//Scheduled saves
        AUTO_SAVE_ID = getServer().getScheduler().scheduleSyncRepeatingTask(
            this,autosaveCircuits,AUTOSAVE_INTERVAL,AUTOSAVE_INTERVAL);

//Setup circuit manager
        circuits = new CircuitManager(new File(this.getDataFolder(),"circuits.yml"),this);

//Enabled msg
        System.out.println("[Quantum Connectors] version " + getDescription().getVersion() + " ENABLED");
    }

    public void msg(Player player,String sMessage){
        player.sendMessage(ChatColor.LIGHT_PURPLE+"[QC] "+ChatColor.WHITE+sMessage);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if(!(sender instanceof Player)){
            System.out.println("This command has to be called by a player");
            return true;
    	}

        Player p = (Player) sender;

        if(args.length == 0 || args[0].equalsIgnoreCase("?")){
            msg(p,"To create a quantum circuit, use /qc <circuit>; and click   on a sender and then a receiver with redstone.");

            String sAvailableCircuits = "";
            for(String sKey : circuitTypes.keySet()){
                sAvailableCircuits += sKey+", ";
            }
            sAvailableCircuits = sAvailableCircuits.substring(0,sAvailableCircuits.length()-2);

            msg(p,ChatColor.YELLOW+"Available circuits: "+ChatColor.WHITE+sAvailableCircuits);

            return true;
        }else if(args[0].equalsIgnoreCase("cancel")){
            if(playerListener.pendingCircuits.containsKey(p)){
                playerListener.pendingCircuits.remove(p);
                playerListener.pendingSenders.remove(p);

                msg(p,"Pending circuit removed!");
            }else{
                msg(p,"No pending circuits");
            }
        }else if(circuitTypes.containsKey(args[0])){
            if(p.hasPermission("QuantumConnectors.create")){
                msg(p,ChatColor.RED+"You don't have permission to create the "+args[0]+" circuit!");
                return true;
            }
            playerListener.pendingCircuits.put(p,circuitTypes.get(args[0]));
            msg(p,"Circuit is ready to be created!");
        }else{
            msg(p,"Invalid circuit specified!");
        }

        return true;
    }

    public void onDisable(){
        circuits.Save();
        getServer().getScheduler().cancelTask(AUTO_SAVE_ID);
    }

    private void setupConfig() {
        Configuration config = new Configuration(new File(this.getDataFolder(),"config.yml"));
        config.load();
        Boolean save = false;

        int iMaxChainLinks = config.getInt("max_chain_links",-1);
        if(iMaxChainLinks == -1){
            iMaxChainLinks = MAX_CHAIN_LINKS;
            config.setProperty("max_chain_links",iMaxChainLinks);
            save = true;
        }
        MAX_CHAIN_LINKS = iMaxChainLinks;

        int iAutoSaveInterval = config.getInt("autosave_minutes",-1);
        if(iAutoSaveInterval == -1){
            iAutoSaveInterval = AUTOSAVE_INTERVAL;
            config.setProperty("autosave_minutes",iAutoSaveInterval);
            save = true;
        }
        AUTOSAVE_INTERVAL = iAutoSaveInterval*60*20;//convert to minutes

        int iChunkUnloadRange = config.getInt("chunk_unload_range",-1);
        if(iChunkUnloadRange == -1){
            iChunkUnloadRange = CHUNK_UNLOAD_RANGE;
            config.setProperty("chunk_unload_range",iChunkUnloadRange);
            save = true;
        }
        CHUNK_UNLOAD_RANGE = iChunkUnloadRange;

        if(save)
            config.save();
    }
    
    public void activateCircuit(Location lSender, int current){
        circuits.activateCircuit(lSender,current,0);
    }

//Scheduled save mechanism
    private Runnable autosaveCircuits = new Runnable() {
        public void run() {
            circuits.Save();
        }
    };
}