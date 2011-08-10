package Ne0nx3r0.QuantumConnectors.Circuit;

import Ne0nx3r0.QuantumConnectors.QuantumConnectors;
import org.bukkit.util.config.Configuration;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.block.Block;
import org.bukkit.Material;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import net.minecraft.server.EntityTNTPrimed;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;

public final class CircuitManager{
    private final QuantumConnectors plugin;
    private Configuration yml;

    private static Map<Location,Circuit> circuits = new HashMap<Location,Circuit>();
    private static List<String> loadedWorlds = new ArrayList<String>();

    private Material[] validSenders = new Material[]{
        Material.LEVER,
        Material.REDSTONE_WIRE,
        Material.STONE_BUTTON,
        Material.STONE_PLATE,
        Material.WOOD_PLATE,
        Material.REDSTONE_TORCH_OFF,
        Material.REDSTONE_TORCH_ON,
        Material.IRON_DOOR_BLOCK,
        Material.WOODEN_DOOR,
        Material.TRAP_DOOR,
        Material.POWERED_RAIL,
        Material.DIODE_BLOCK_OFF,
        Material.DIODE_BLOCK_ON
    };
    private Material[] validReceivers = new Material[]{
        Material.LEVER,
        Material.IRON_DOOR_BLOCK,
        Material.WOODEN_DOOR,
        Material.TRAP_DOOR,
        Material.POWERED_RAIL,
        Material.TNT
    };

    public CircuitManager(File ymlFile, final QuantumConnectors qc){
        plugin = qc;
        
        yml = new Configuration(ymlFile);
        reload();

        Load();
        
        preloadChunks();
    }
    public void reload() {
        yml.load();
    }

    // Fixed to new circuit layout
    public boolean circuitExists(Location lSender){
        return circuits.containsKey(lSender);
    }
    public Set<Location> circuitLocations() {
        return circuits.keySet();
    }
    public Circuit getCircuit(Location lSender){
        return circuits.get(lSender);
    }
    public void addCircuit(Location lSender,Location lReceiver,int iType){
        circuits.put(lSender,new Circuit(iType, lReceiver));
    }
    public void addCircuit(Location sender, List<Location> receivers, int type) {
        circuits.put(sender, new Circuit(type, receivers));
    }
    public void removeCircuit(Location lSender){
        if(circuitExists(lSender))
            circuits.remove(lSender);
    }

    // Activate
    public void activateCircuit(Location lSender,int current,int chain){
        Circuit circuit = getCircuit(lSender);

        for(Location receiver : circuit.getReceivers()) {
            Block b = receiver.getBlock();

            if(isValidReceiver(b)){
                int iType = circuit.type;

                if(b.getType() == Material.TNT) // TnT is one time use!
                    removeCircuit(lSender);

                if(iType == plugin.typeQuantum){
                    setReceiver(b, current>0?true:false);
                }else if(iType == plugin.typeOn){
                    if(current > 0)
                        setReceiver(b, true);
                }else if(iType == plugin.typeOff){
                    if(current > 0)
                        setReceiver(b, false);
                }else if(iType == plugin.typeToggle){
                    if(current > 0)
                        setReceiver(b, getBlockCurrent(b)>0?false:true);
                }else if(iType == plugin.typeReverse){
                    setReceiver(b, current>0?false:true);
                }else if(iType == plugin.typeRandom){
                    if(current > 0){
                        setReceiver(b, new Random().nextBoolean()?true:false);
                    }
                }

                //allow zero to be infinite
                if(plugin.getChain() > 0) chain++;
                if(chain <= plugin.getChain() && circuitExists(b.getLocation()))
                    activateCircuit(b.getLocation(),getBlockCurrent(b),chain);
            }else{
                // Don't want to remove circuit, want to remove receiver
                removeCircuit(lSender);
            }
        }
    }
    public int getBlockCurrent(Block b) {
        Material mBlock = b.getType();
        int iData = (int) b.getData();

        if(mBlock == Material.LEVER
            || mBlock == Material.POWERED_RAIL)
            return (iData&0x08)==0x08?15:0;

        else if(mBlock == Material.IRON_DOOR_BLOCK 
            || mBlock == Material.WOODEN_DOOR
            || mBlock == Material.TRAP_DOOR)    
            return (iData&0x04)==0x04?15:0;
        
        return b.getBlockPower();
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
        }else if(mBlock == Material.TNT) {
            block.setType(Material.AIR);
            CraftWorld world = (CraftWorld)block.getWorld();
            EntityTNTPrimed tnt = new EntityTNTPrimed(world.getHandle(), block.getX() + 0.5F, block.getY() + 0.5F, block.getZ() + 0.5F);
            world.getHandle().addEntity(tnt);
            block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 1);
        }
    }

    // Sender/Receiver checks
    public boolean isValidSender(Block block){
        Material mBlock = block.getType();
        for(int i=0;i<validSenders.length;i++)
            if(mBlock == validSenders[i])
                return true;

        return false;
    }
    public String getValidSendersString(){
        String msg = "";
        for(int i=0;i<validSenders.length;i++)
            msg += (i!=0?", ":"")+validSenders[i].name().toLowerCase().replace("_"," ");

        return msg;
    }
    public boolean isValidReceiver(Block block){
        Material mBlock = block.getType();
        for(int i=0;i<validReceivers.length;i++)
            if(mBlock == validReceivers[i])
                return true;

        return false;
    }
    public String getValidReceiversString(){
        String msg = "";
        for(int i=0;i<validReceivers.length;i++)
            msg += (i!=0?", ":"")+validReceivers[i].name().toLowerCase().replace("_"," ");
        
        return msg;
    }

    // Load old/new syntax
    private void Load() {
        if(yml.getProperty("circuits") != null) { // Old Circuitry exists!
            loadOld();
            // Delete so it doesn't load again
            yml.removeProperty("circuits");
            yml.save();
        }

        // Load new Syntax
        for(World world : plugin.getServer().getWorlds())
            if(!loadedWorlds.contains(world.getName())) { // Only load if world isn't already loaded
                if(yml.getProperty(world.getName()) != null) { // If world exists, loop through circuits
                    for(int x = 0; ; x++) {
                        String path = world.getName()+".circuit_"+x;
                        if(yml.getProperty(path) == null) break; // If it doesn't exist, break the loop for the world

                        List<Location> list = new ArrayList<Location>();
                        for(Object xyz : yml.getList(path+".receivers")) {
                            String[] loc = xyz.toString().split(","); // Iterated for multi-world... maybe?
                            list.add(getLocation(plugin.getServer().getWorld(loc[3]), loc));
                        }
                        
                        addCircuit(getLocation(world, yml.getString(path+".sender").split(",")),
                                list, yml.getInt(path+".type",0)); // Input the circuit

                    }
                }
                loadedWorlds.add(world.getName());
            }
    }
    public void loadWorld(World world) {
        String name = world.getName();
        if(!loadedWorlds.contains(name)) {
            if(yml.getProperty(name) != null) {
                for(int x = 0; ; x++) {
                    String path = name+".circuit_"+x;
                    if(yml.getProperty(path) == null) break;

                    List<Location> list = new ArrayList<Location>();
                    for(Object xyz : yml.getList(path+".receivers"))
                        list.add(getLocation(world, xyz.toString().split(",")));

                    addCircuit(getLocation(world, yml.getString(path+".sender").split(",")),
                            list, yml.getInt(path+".type",0)); // Input the circuit
                }
            }
            loadedWorlds.add(name);
        }
    }
    private Location getLocation(World world, String[] xyz) {
        if(xyz.length < 3) return null;
        return new Location(world, Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
    }
    // The old mess...
    public void loadOld(){
        List tempCircuits = yml.getList("circuits");

        Server server = plugin.getServer();
        Location lSender;
        Location lReceiver;
        int iType;
        Map<String,Object> temp;
        for(int i=0; i< tempCircuits.size(); i++){
            temp = (Map<String,Object>) tempCircuits.get(i);

            iType = (Integer) temp.get("type");

            lSender = new Location(
                server.getWorld((String) temp.get("sw")),
                (Integer) temp.get("sx"),
                (Integer) temp.get("sy"),
                (Integer) temp.get("sz")
            );

            lReceiver = new Location(
                server.getWorld((String) temp.get("rw")),
                (Integer) temp.get("rx"),
                (Integer) temp.get("ry"),
                (Integer) temp.get("rz")
            );

            // Removed check, it's checked on activation anyway.
            addCircuit(lSender,lReceiver,iType);
	}
    }

    // Save world data on unload and remove the circuits from memory.
    public void saveWorld(String world) {
        if(yml.getProperty(world) != null) yml.removeProperty(world); // Remove world if it exists
        int count = 0;
        for(Location key : circuitLocations()) 
            if (key.getWorld().toString().equals(world)) {
            Circuit circuit = getCircuit(key);
            String path = world+".circuit_"+count;

            yml.setProperty(path+".type", circuit.type); // Save Type
            yml.setProperty(path+".sender", 
                    key.getBlockX()+","+key.getBlockY()+","+key.getBlockZ()); // Save sender location
            
            List <String> temp = new ArrayList<String>();
            for(Location loc : circuit.getReceivers()) // Loop through receivers
                temp.add(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()+","+loc.getWorld().getName());

            yml.setProperty(path+".receivers", temp); // Save receiver list

            removeCircuit(key); // Remove circuit from memory

            count += 1;
        }
        loadedWorlds.remove(world); // World isn't loaded anymore.
        yml.save();
    }
    // Save all data
    public void Save(){
        Map<String,Integer> worldCount = new HashMap<String,Integer>();

        for (World world : plugin.getServer().getWorlds())
            if(yml.getProperty(world.getName()) != null) // World Exists
                yml.removeProperty(world.getName()); // Remove world circuits

        for(Location key : circuitLocations()){
            Circuit currentCircuit = getCircuit(key);
            String wName = key.getWorld().getName(); // Get world name
            int count = worldCount.get(wName)==null?0:worldCount.get(wName); // Get world circuit ID
            String path  = wName+".circuit_"+count; // Set base path

            yml.setProperty(path+".type", currentCircuit.type); // Save Type
            yml.setProperty(path+".sender", 
                    key.getBlockX()+","+key.getBlockY()+","+key.getBlockZ()); // Save sender location

            List <String> temp = new ArrayList<String>();
            for(Location loc : currentCircuit.getReceivers()) // Loop through receivers
                temp.add(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()+","+loc.getWorld().getName());

            yml.setProperty(path+".receivers", temp); // Save receiver list

            worldCount.put(wName, count+1); // Increase Count
        }
        yml.save();
    }
    
    
    // Loads chunks that contain circuits (with a range around them as well).
    private void preloadChunks() {
        for (Location loc : circuitLocations()) {
            // Don't need to load sender...!
            for(Location l : getCircuit(loc).getReceivers()) {
                // get the center chunk from the block
                Chunk center = loc.getBlock().getChunk();
                // get the world from the chunk
                World world = center.getWorld();
                // get our surrounding range
                int range = plugin.getChunkUnloadRange();

                // iterate over the matrix of blocks that make up the center (circuit) block's chunk and the chunks within the "range"
                for (int dx = -(range); dx <= range; dx++) {
                    for (int dz = -(range); dz <= range; dz++) {
                        // load the chunk
                        Chunk chunk = world.getChunkAt(center.getX() + dx,
                                                       center.getZ() + dz);
                        world.loadChunk(chunk);
                    }
                }
            }
        }
    }
}