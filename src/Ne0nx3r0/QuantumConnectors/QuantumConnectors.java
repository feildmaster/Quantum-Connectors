package Ne0nx3r0.QuantumConnectors;

import java.io.File;

import org.bukkit.Chunk;
import org.bukkit.World;
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
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import java.util.Random;
import org.bukkit.Location;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.logging.Logger;

public class QuantumConnectors extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Minecraft");
    private final QuantumConnectorsBlockListener blockListener = new QuantumConnectorsBlockListener(this);
    private final QuantumConnectorsPlayerListener playerListener = new QuantumConnectorsPlayerListener(this);
    private final QuantumConnectorsWorldListener worldListener = new QuantumConnectorsWorldListener(this);

    public static Map<String,Integer> circuitTypes = new HashMap<String,Integer>();

    public static PermissionHandler permissionHandler;

    public static CircuitManager circuits;

    private static int AUTO_SAVE_ID = -1;

    public static boolean USING_PERMISSIONS = false;

    public int typeQuantum = 0;
    public int typeOn = 1;
    public int typeOff = 2;
    public int typeToggle = 3;
    public int typeReverse = 4;
    public int typeRandom = 5;

//Configurables
    private int MAX_CHAIN_LINKS = 3;
    private int AUTOSAVE_INTERVAL = 10;//specified here in minutes
    private int CHUNK_UNLOAD_RANGE = 5; //number of chunks surrounding the circuit to keep around when unloading chunks

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

//Setup circuit manager
        circuits = new CircuitManager(new File(this.getDataFolder(),"circuits.yml"),this);

//Configuration
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

//Scheduled saves
        AUTO_SAVE_ID = getServer().getScheduler().scheduleSyncRepeatingTask(
            this,autosaveCircuits,AUTOSAVE_INTERVAL,AUTOSAVE_INTERVAL);

//Permissions
        setupPermissions();

//Load chunks that contain circuits
        preloadCircuitChunks();

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

        Player pSender = (Player) sender;

        if(args.length == 0 || args[0].equalsIgnoreCase("?")){
            msg(pSender,"To create a quantum circuit, use /qc <circuit>; and click   on a sender and then a receiver with redstone.");

            String sAvailableCircuits = "";
            for(String sKey : circuitTypes.keySet()){
                sAvailableCircuits += sKey+", ";
            }
            sAvailableCircuits = sAvailableCircuits.substring(0,sAvailableCircuits.length()-2);

            msg(pSender,ChatColor.YELLOW+"Available circuits: "+ChatColor.WHITE+sAvailableCircuits);

            return true;
        }else if(args[0].equalsIgnoreCase("cancel")){
            if(playerListener.pendingCircuits.containsKey(pSender)){
                playerListener.pendingCircuits.remove(pSender);
                playerListener.pendingSenders.remove(pSender);

                msg(pSender,"Pending circuit removed!");
            }else{
                msg(pSender,"No pending circuits");
            }
        }else if(args[0] != null){
            if(circuitTypes.containsKey(args[0])){
                if(this.USING_PERMISSIONS && !permissionHandler.has(pSender, "QuantumConnectors.create."+args[0])){
                    msg(pSender,ChatColor.RED+"You don't have permission to create the "+args[0]+" circuit!");
                    return true;
                }

                playerListener.pendingCircuits.put(pSender,circuitTypes.get(args[0]));

                msg(pSender,"Circuit is ready to be created!");
            }else{
                msg(pSender,"Invalid circuit specified!");
            }
        }else{
            msg(pSender,"Invalid qc command.");
        }

        return true;
    }

    public void onDisable(){
        circuits.Save();

        getServer().getScheduler().cancelTask(AUTO_SAVE_ID);
    }

    public void activateCircuit(Location lSender,int current){
        activateCircuit(lSender,current,0);
    }

    //would have preferred to put these someplace else, but I couldn't find a convenient spot
    public void activateCircuit(Location lSender,int current,int chain){
        Circuit circuit = circuits.getCircuit(lSender);

        Block bReceiver = circuit.reciever.getBlock();

        if(circuits.isValidReceiver(bReceiver)){
            int iType = circuit.type;

            if(iType == typeQuantum){
                if(current > 0){
                    setOn(bReceiver);
                }else{
                    setOff(bReceiver);
                }
            }else if(iType == typeOn){
                if(current > 0){
                    setOn(bReceiver);
                }
            }else if(iType == typeOff){
                if(current > 0){
                    setOff(bReceiver);
                }
            }else if(iType == typeToggle){
                if(current > 0){
                    if(getBlockCurrent(bReceiver) > 0){
                        setOff(bReceiver);
                    }else{
                        setOn(bReceiver);
                    }
                }
            }else if(iType == typeReverse){
                if(current > 0){
                    setOff(bReceiver);
                }else{
                    setOn(bReceiver);
                }
            }else if(iType == typeRandom){
                if(current > 0){
                    Random randomGenerator = new Random();

                    if(randomGenerator.nextBoolean()){
                        setOn(bReceiver);
                    }else{
                        setOff(bReceiver);
                    }
                }
            }

            //allow zero to be infinite
            if(MAX_CHAIN_LINKS > 0){
                chain++;
            }
            if(chain <= MAX_CHAIN_LINKS && circuits.circuitExists(bReceiver.getLocation())){
                activateCircuit(bReceiver.getLocation(),getBlockCurrent(bReceiver),chain);
                //pass an array of the locations used so far instead of the chain count, so it can't loop
            }
        }else{
            circuits.removeCircuit(lSender);
        }

    }

    public int getBlockCurrent(Block bReceiver){
        Material mBlock = bReceiver.getType();
        int iData = (int) bReceiver.getData();

        if(mBlock == Material.LEVER
            || mBlock == Material.POWERED_RAIL){
            if((iData&0x08) == 0x08)
                return 15;
            else
                return 0;
        }else if(mBlock == Material.IRON_DOOR_BLOCK 
            || mBlock == Material.WOODEN_DOOR
            || mBlock == Material.TRAP_DOOR){
            if((iData&0x04) == 0x04)
                return 15;
            else
                return 0;
        }
        return bReceiver.getBlockPower();
    }

    private void setOn(Block block){
        setReceiver(block,true);
    }
    private void setOff(Block block){
        setReceiver(block,false);
    }

    private void setReceiver(Block block,boolean on){
        Material mBlock = block.getType();
        int iData = (int) block.getData();

        if(mBlock == Material.LEVER || mBlock == Material.POWERED_RAIL){
            if(on && (iData&0x08) != 0x08) iData|=0x08; //send power on
            else if(!on && (iData&0x08) == 0x08)iData^=0x08; //send power off
                
            block.setData((byte) iData);
        }else if(mBlock == Material.IRON_DOOR_BLOCK || mBlock == Material.WOODEN_DOOR){
            Block bOtherPiece = block.getRelative(((iData&0x08) == 0x08)?BlockFace.DOWN:BlockFace.UP);
            int iOtherPieceData = (int) bOtherPiece.getData();

            if(on && (iData&0x04) != 0x04){
                iData|=0x04;
                iOtherPieceData|=0x04;
            }else if(!on && (iData&0x04) == 0x04){
                iData^=0x04;
                iOtherPieceData^=0x04;
            }
            block.setData((byte) iData);
            bOtherPiece.setData((byte) iOtherPieceData);
        }else if(mBlock == Material.TRAP_DOOR){
            if(on && (iData&0x04) != 0x04) iData|=0x04;//send open
            else if(!on && (iData&0x04) == 0x04) iData^=0x04;//send close
            
            block.setData((byte) iData);
        }
    }

//Scheduled save mechanism
    private Runnable autosaveCircuits = new Runnable() {
        public void run() {
            circuits.Save();
        }
    };

    private void setupPermissions() {
        Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

        if (this.permissionHandler == null) {
            if(permissionsPlugin != null){
                USING_PERMISSIONS = true;
                this.permissionHandler = ((Permissions) permissionsPlugin).getHandler();
                System.out.println("[Quantum Connectors] Integrated with Permissions");
            }
        }
    }

    /**
     * Retrieves a handle to the logger we're using for this plugin
     *
     * @return the Logger
     */
    public Logger getLogger()
    {
        return log;
    }

    /**
     * Gets the chunk unload range that was specified in the config
     *
     * @return the range
     */
    public int getChunkUnloadRange()
    {
        return CHUNK_UNLOAD_RANGE;
    }

// Loads chunks that contain circuits (with a range around them as well).
    private void preloadCircuitChunks()
    {
        for (Location loc : CircuitManager.getCircuitLocations())
        {
            // get the center chunk from the block
            Chunk center = loc.getBlock().getChunk();
            // get the world from the chunk
            World world = center.getWorld();
            // get our surrounding range
            int range = getChunkUnloadRange();

            // iterate over the matrix of blocks that make up the center (circuit) block's chunk and the chunks within the "range"
            for (int dx = -(range); dx <= range; dx++)
            {
                for (int dz = -(range); dz <= range; dz++)
                {
                    // load the chunk
                    Chunk chunk = world.getChunkAt(center.getX() + dx,
                                                   center.getZ() + dz);
                    world.loadChunk(chunk);
                }
            }
        }
    }
}