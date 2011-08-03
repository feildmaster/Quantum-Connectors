package Ne0nx3r0.QuantumConnectors.Manager;

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
import java.util.Set;
import org.bukkit.World;

public final class CircuitManager{
    private final QuantumConnectors plugin;
    public static Configuration yml;

    private static Map<Location,Circuit> circuits;
    private List<String> loadedWorlds = new ArrayList<String>();

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

    public CircuitManager(File ymlFile,final QuantumConnectors plugin){
        this.plugin = plugin;

        circuits = new HashMap<Location,Circuit>();
        
        yml = new Configuration(ymlFile);
        yml.load();

        Load();
    }

    // Fixed to new circuit layout
    public boolean circuitExists(Location lSender){
        return circuits.containsKey(lSender);
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
        if(circuits.containsKey(lSender)){
            circuits.remove(lSender);
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
    public void Load() {
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
                        for(Object xyz : yml.getList(path+".receivers"))
                            list.add(getLocation(world, xyz.toString().split(",")));

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
        return new Location(world, Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
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

            if(isValidSender(lSender.getBlock()) 
            && isValidReceiver(lReceiver.getBlock())
            && plugin.circuitTypes.containsValue(iType)
			&& !lReceiver.toString().equals(lSender.toString())){
                addCircuit(lSender,lReceiver,iType);
            }else{
                System.out.println("[QuantumConnectors] Removing invalid circuit.");
            }
	}
    }

    // Save world data on unload and remove the circuits from memory.
    public void saveWorld(String world) {
        if(yml.getProperty(world) != null) yml.removeProperty(world); // Remove world if it exists
        int count = 0;
        for(Location key : circuits.keySet()) {
            Circuit circuit = circuits.get(key);
            String path = world+".circuit_"+count;

            yml.setProperty(path+".type", circuit.type); // Save Type
            yml.setProperty(path+".sender", key); // Save sender location

            // ToDo: Find out if saving locations, or doing the current loops is better
            List <String> temp = new ArrayList<String>();

            for(Location loc : circuit.getReceivers()) // Loop through receivers
                temp.add(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());

            yml.setProperty(path+".receivers", temp); // Save receiver list

            circuits.remove(key); // Remove circuit from memory

            count++;
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

        for(Location key : circuits.keySet()){
            Circuit currentCircuit = circuits.get(key);
            String wName = key.getWorld().getName(); // Get world name
            int count = worldCount.get(wName)==null?0:worldCount.get(wName); // Get world circuit ID
            String path  = wName+".circuit_"+count; // Set base path
            System.out.println("Circuit being saved ("+wName+") ["+count+"]");

            yml.setProperty(path+".type", currentCircuit.type); // Save Type
            yml.setProperty(path+".sender", 
                    key.getBlockX()+","+key.getBlockY()+","+key.getBlockZ()); // Save sender location

            List <String> temp = new ArrayList<String>();
            for(Location loc : currentCircuit.getReceivers()) // Loop through receivers
                temp.add(loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());

            yml.setProperty(path+".receivers", temp); // Save receiver list

            worldCount.put(wName, count+1); // Increase Count
        }
        yml.save();
    }

    public static Set<Location> getCircuitLocations()
    {
        return circuits.keySet();
    }
}
