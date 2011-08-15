package feildmaster.QuantumConnectors;

import feildmaster.QuantumConnectors.Listeners.*;
import feildmaster.QuantumConnectors.Circuit.*;
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

    // Public "gets"
    public void msg(Player player,String sMessage){
        player.sendMessage(ChatColor.LIGHT_PURPLE+"[QC] "+ChatColor.WHITE+sMessage);
    }
    public int getChain() {
        return MAX_CHAIN_LINKS;
    }
    public int getChunkUnloadRange() {
        return CHUNK_UNLOAD_RANGE;
    }
    
    public void onEnable() {
//Register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Priority.Low, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.WORLD_UNLOAD, worldListener, Priority.Monitor, this);
        
//Setup circuits
        circuitTypes.put("quantum",typeQuantum);
        circuitTypes.put("on",typeOn);
        circuitTypes.put("off",typeOff);
        circuitTypes.put("toggle",typeToggle);
        circuitTypes.put("reverse",typeReverse);
        circuitTypes.put("random",typeRandom);

//Configuration
        loadConfig();

//Scheduled saves
        AUTO_SAVE_ID = getServer().getScheduler().scheduleSyncRepeatingTask(
            this,autosaveCircuits,AUTOSAVE_INTERVAL,AUTOSAVE_INTERVAL);

//Setup circuit manager
        circuits = new CircuitManager(new File(this.getDataFolder(),"circuits.yml"),this);

//Enabled msg
        System.out.println("[Quantum Connectors] version " + getDescription().getVersion() + " ENABLED");
    }
    public void onDisable(){
        circuits.Save();
        getServer().getScheduler().cancelTask(AUTO_SAVE_ID);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if(!(sender instanceof Player)){
            System.out.println("This command has to be called by a player");
            return true;
    	}

        Player p = (Player) sender;

        if(args.length == 0 || args[0].equalsIgnoreCase("?")){
            msg(p,"To create a quantum circuit, use /qc <circuit>; and click   on a sender and then a receiver with redstone.");

            String s = "";
            for(String sKey : circuitTypes.keySet()){
                s += sKey+", ";
            }
            
            msg(p,ChatColor.YELLOW+"Available circuits: "+ChatColor.WHITE+s.substring(0,s.length()-2));
        }else if(args[0].equalsIgnoreCase("cancel")){
            if(playerListener.pendingCircuits.containsKey(p)){
                playerListener.pendingCircuits.remove(p);
                playerListener.pendingSenders.remove(p);

                msg(p,"Pending circuit removed!");
            }else{
                msg(p,"No pending circuits");
            }
        }else if(circuitTypes.containsKey(args[0])){
            if(p.hasPermission("QuantumConnectors.create."+args[0].toLowerCase())){
                playerListener.pendingCircuits.put(p,circuitTypes.get(args[0].toLowerCase()));
                msg(p,"Circuit is ready to be created!");
            } else {
                msg(p,ChatColor.RED+"You don't have permission to create the "+args[0]+" circuit!");
            }
        }else{
            msg(p,"Invalid circuit specified!");
        }

        return true;
    }

    private void loadConfig() {
        Configuration config = getConfiguration();
        MAX_CHAIN_LINKS = config.getInt("max_chain_links",MAX_CHAIN_LINKS);
        AUTOSAVE_INTERVAL = config.getInt("autosave_minutes", AUTOSAVE_INTERVAL)*60*20;//convert to minutes
        CHUNK_UNLOAD_RANGE = config.getInt("chunk_unload_range",CHUNK_UNLOAD_RANGE);
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