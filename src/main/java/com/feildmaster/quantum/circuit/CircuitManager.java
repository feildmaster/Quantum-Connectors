package com.feildmaster.quantum.circuit;

import com.feildmaster.lib.configuration.EnhancedConfiguration;
import com.feildmaster.quantum.QuantumConnectors;
import com.feildmaster.quantum.QuantumConnectors.Type;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.block.Block;
import org.bukkit.Material;
import java.util.List;
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

public final class CircuitManager {
    private final QuantumConnectors plugin;
    private EnhancedConfiguration yml;
    private static Map<Location, Circuit> circuits = new HashMap<Location, Circuit>();
    private Map<Location, List<String[]>> pendingReceivers = new HashMap<Location, List<String[]>>();
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
        Material.POWERED_RAIL,};
    private Material[] validReceivers = new Material[]{
        Material.LEVER,
        Material.IRON_DOOR_BLOCK,
        Material.WOODEN_DOOR,
        Material.TRAP_DOOR,
        Material.POWERED_RAIL,
        //Material.PISTON_BASE,
        //Material.PISTON_STICKY_BASE,
        Material.TNT,};

    public CircuitManager(File ymlFile, final QuantumConnectors qc) {
        plugin = qc;

        yml = new EnhancedConfiguration(ymlFile, qc);

        Load();

        preloadChunks();
    }

    public void reload() {
        yml.load();
    }

    // Fixed to new circuit layout
    public boolean circuitExists(Location l) {
        return circuits.containsKey(l);
    }

    public boolean receiverExists(Location l) {
        for (Circuit c : circuits.values()) {
            if (c.getReceivers().contains(l)) {
                return true;
            }
        }

        return false;
    }

    public Set<Location> circuitLocations() {
        return circuits.keySet();
    }

    public Circuit getCircuit(Location lSender) {
        return circuits.get(lSender);
    }

    public void addCircuit(Location lSender, Location lReceiver, int iType) {
        circuits.put(lSender, new Circuit(iType, lReceiver));
    }

    public void addCircuit(Location sender, List<Location> receivers, int type) {
        circuits.put(sender, new Circuit(type, receivers));
    }

    public void removeCircuit(Location lSender) {
        if (circuitExists(lSender)) {
            circuits.remove(lSender);
        }
    }

    public void removeReceiver(Location l) {
        while (receiverExists(l)) {
            for (Location key : circuitLocations()) {
                Circuit c = getCircuit(key);
                if (c.getReceivers().contains(l)) {
                    c.delReceiver(l);
                    if (c.getReceivers().isEmpty()) {
                        removeCircuit(key);
                    }
                    break;
                }
            }
        }
    }

    // Activate
    public void activateCircuit(Location lSender, int current, int chain) {
        Circuit circuit = getCircuit(lSender);
        List<Location> receivers = circuit.getReceivers();
        int iType = circuit.type;

        if (receivers.isEmpty()) {
            return;
        }
        for (Location r : receivers) {
            Block b = r.getBlock();

            if (isValidReceiver(b)) {
                if (iType == Type.QUANTUM.getId()) {
                    setReceiver(b, current > 0 ? true : false);
                } else if (iType == Type.ON.getId()) {
                    if (current > 0) {
                        setReceiver(b, true);
                    }
                } else if (iType == Type.OFF.getId()) {
                    if (current > 0) {
                        setReceiver(b, false);
                    }
                } else if (iType == Type.TOGGLE.getId()) {
                    if (current > 0) {
                        setReceiver(b, getBlockCurrent(b) > 0 ? false : true);
                    }
                } else if (iType == Type.REVERSE.getId()) {
                    setReceiver(b, current > 0 ? false : true);
                } else if (iType == Type.RANDOM.getId()) {
                    if (current > 0) {
                        setReceiver(b, new Random().nextBoolean() ? true : false);
                    }
                }

                if (b.getType() == Material.TNT) { // TnT is one time use!
                    removeReceiver(r);
                }

                if (plugin.getChain() > 0) { //allow zero to be infinite
                    chain++;
                }
                if (chain <= plugin.getChain() && circuitExists(b.getLocation())) {
                    activateCircuit(b.getLocation(), getBlockCurrent(b), chain);
                }
            } else {
                removeReceiver(r);
            }
        }

    }

    public int getBlockCurrent(Block b) {
        Material mBlock = b.getType();
        int iData = (int) b.getData();

        if (mBlock == Material.LEVER
                || mBlock == Material.POWERED_RAIL) {
            return (iData & 0x08) == 0x08 ? 15 : 0;
        } else if (mBlock == Material.IRON_DOOR_BLOCK
                || mBlock == Material.WOODEN_DOOR
                || mBlock == Material.TRAP_DOOR) {
            return (iData & 0x04) == 0x04 ? 15 : 0;
        }

        return b.getBlockPower();
    }

    private void setReceiver(Block block, boolean on) {
        Material mBlock = block.getType();
        int iData = (int) block.getData();

        if (mBlock == Material.LEVER) {
            if (on && (iData & 0x08) != 0x08) { // Massive annoyance
                iData |= 0x08; //send power on
            } else if (!on && (iData & 0x08) == 0x08) {
                iData ^= 0x08; //send power off
            }
            int i1 = iData & 7;
            net.minecraft.server.World w = ((net.minecraft.server.World) ((CraftWorld) block.getWorld()).getHandle());
            Location l = block.getLocation();
            int i = (int) l.getX();
            int j = (int) l.getY();
            int k = (int) l.getZ();
            int id = block.getTypeId();
            w.setData(i, j, k, iData);
            w.applyPhysics(i, j, k, id);
            if (i1 == 1) {
                w.applyPhysics(i - 1, j, k, id);
            } else if (i1 == 2) {
                w.applyPhysics(i + 1, j, k, id);
            } else if (i1 == 3) {
                w.applyPhysics(i, j, k - 1, id);
            } else if (i1 == 4) {
                w.applyPhysics(i, j, k + 1, id);
            } else {
                w.applyPhysics(i, j - 1, k, id);
            }
        } else if (mBlock == Material.POWERED_RAIL) {
            if (on && (iData & 0x08) != 0x08) {
                iData |= 0x08; //send power on
            } else if (!on && (iData & 0x08) == 0x08) {
                iData ^= 0x08; //send power off
            }
            block.setData((byte) iData);
        } else if (mBlock == Material.IRON_DOOR_BLOCK || mBlock == Material.WOODEN_DOOR) {
            Block bOtherPiece = block.getRelative(((iData & 0x08) == 0x08) ? BlockFace.DOWN : BlockFace.UP);
            int iOtherPieceData = (int) bOtherPiece.getData();

            if (on && (iData & 0x04) != 0x04) {
                iData |= 0x04;
                iOtherPieceData |= 0x04;
            } else if (!on && (iData & 0x04) == 0x04) {
                iData ^= 0x04;
                iOtherPieceData ^= 0x04;
            }
            block.setData((byte) iData);
            bOtherPiece.setData((byte) iOtherPieceData);
            block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0, 10);
        } else if (mBlock == Material.TRAP_DOOR) {
            if (on && (iData & 0x04) != 0x04) {
                iData |= 0x04;//send open
            } else if (!on && (iData & 0x04) == 0x04) {
                iData ^= 0x04;//send close
            }
            block.setData((byte) iData);
        } else if (mBlock == Material.TNT) {
            block.setType(Material.AIR);
            CraftWorld world = (CraftWorld) block.getWorld();
            EntityTNTPrimed tnt = new EntityTNTPrimed(world.getHandle(), block.getX() + 0.5F, block.getY() + 0.5F, block.getZ() + 0.5F);
            world.getHandle().addEntity(tnt);
            block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 1);
        } else if (mBlock == Material.PISTON_BASE || mBlock == Material.PISTON_STICKY_BASE) {
            // Makeshift piston code... Doesn't work!
            if (on && (iData & 0x08) != 0x08) {
                iData |= 0x08; //send power on
            } else if (!on && (iData & 0x08) == 0x08) {
                iData ^= 0x08; //send power off
            }
            block.setData((byte) iData);
            //net.minecraft.server.Block.PISTON.doPhysics(((CraftWorld)block.getWorld()).getHandle(), block.getX(), block.getY(), block.getZ(), -1);
        } else if (mBlock == Material.REDSTONE_TORCH_ON) {
            if (!on) {
                block.setType(Material.REDSTONE_TORCH_OFF);
            }
        } else if (mBlock == Material.REDSTONE_TORCH_OFF) {
            if (on) {
                block.setType(Material.REDSTONE_TORCH_ON);
            }
        }
    }

    // Sender/Receiver checks
    public boolean isValidSender(Block block) {
        Material mBlock = block.getType();
        for (int i = 0; i < validSenders.length; i++) {
            if (mBlock == validSenders[i]) {
                return true;
            }
        }

        return false;
    }

    public String getValidSendersString() {
        String msg = "";
        for (int i = 0; i < validSenders.length; i++) {
            msg += (i != 0 ? ", " : "") + validSenders[i].name().toLowerCase().replace("_", " ");
        }

        return msg;
    }

    public boolean isValidReceiver(Block block) {
        Material mBlock = block.getType();
        for (int i = 0; i < validReceivers.length; i++) {
            if (mBlock == validReceivers[i]) {
                return true;
            }
        }

        return false;
    }

    public String getValidReceiversString() {
        String msg = "";
        for (int i = 0; i < validReceivers.length; i++) {
            msg += (i != 0 ? ", " : "") + validReceivers[i].name().toLowerCase().replace("_", " ");
        }

        return msg;
    }

    // Load old/new syntax
    private void Load() {
        // Load new Syntax
        for (World world : plugin.getServer().getWorlds()) {
            if (!loadedWorlds.contains(world.getName())) { // Only load if world isn't already loaded
                if (yml.get(world.getName()) != null) { // If world exists, loop through circuits
                    for (int x = 0;; x++) {
                        String path = world.getName() + ".circuit_" + x;
                        if (yml.get(path) == null) {
                            break; // If it doesn't exist, break the loop for the world
                        }
                        List<Location> list = new ArrayList<Location>();
                        List<String[]> pend = new ArrayList<String[]>();
                        for (Object xyz : yml.getList(path + ".receivers")) {
                            String[] loc = xyz.toString().split(",");

                            World w = plugin.getServer().getWorld(loc[3]);
                            if (w != null) // The world is loaded!
                            {
                                list.add(getLocation(plugin.getServer().getWorld(loc[3]), loc));
                            } else // Save to pending!
                            {
                                pend.add(loc);
                            }
                        }

                        Location cl = getLocation(world, yml.getString(path + ".sender").split(","));
                        addCircuit(cl, list, yml.getInt(path + ".type", 0)); // Input the circuit

                        if (!pend.isEmpty()) {
                            pendingReceivers.put(cl, pend);
                        }
                    }
                }
                loadedWorlds.add(world.getName());
            }
        }
    }

    public void loadWorld(World world) {
        String name = world.getName();
        if (!loadedWorlds.contains(name)) {
            if (yml.get(name) != null) {
                for (int x = 0;; x++) {
                    String path = name + ".circuit_" + x;
                    if (yml.get(path) == null) {
                        break;
                    }

                    List<Location> list = new ArrayList<Location>();
                    List<String[]> pend = new ArrayList<String[]>();
                    for (Object xyz : yml.getList(path + ".receivers")) {
                        String[] loc = xyz.toString().split(",");
                        if (loc[3].equals(name)) { // We're already in the world!
                            list.add(getLocation(world, loc));
                        } else if (loadedWorlds.contains(loc[3])) { // Worlds loaded
                            list.add(getLocation(plugin.getServer().getWorld(loc[3]), loc));
                        } else { // Set as pending...
                            pend.add(loc);
                        }
                    }

                    // Loop through any pending Receivers
                    for (Location l : pendingReceivers.keySet()) {
                        for (String[] s : pendingReceivers.get(l)) {
                            if (s[3].equals(name)) {
                                getCircuit(l).addReceiver(getLocation(world, s));
                            }
                        }
                    }

                    Location cl = getLocation(world, yml.getString(path + ".sender").split(","));
                    addCircuit(cl, list, yml.getInt(path + ".type", 0)); // Input the circuit

                    if (!pend.isEmpty()) {
                        pendingReceivers.put(cl, pend);
                    }
                }
            }
            loadedWorlds.add(name);
        }
    }

    private Location getLocation(World world, String[] xyz) {
        if (xyz.length < 3) {
            return null;
        }
        return new Location(world, Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
    }

    // Save world data on unload and remove the circuits from memory.
    public void saveWorld(String world) {
        if (yml.get(world) != null) {
            yml.set(world, null); // Remove world if it exists
        }

        int count = 0;
        for (Location key : circuitLocations()) {
            if (key.getWorld().toString().equals(world)) {
                Circuit circuit = getCircuit(key);
                String path = world + ".circuit_" + count;

                yml.set(path + ".type", circuit.type); // Save Type
                yml.set(path + ".sender", key.getBlockX() + "," + key.getBlockY() + "," + key.getBlockZ()); // Save sender location

                List<String> temp = new ArrayList<String>();
                for (Location loc : circuit.getReceivers()) // Loop through receivers
                {
                    temp.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getWorld().getName());
                }

                if (pendingReceivers.containsKey(key)) {
                    for (String[] s : pendingReceivers.get(key)) {
                        temp.add(s[0] + "," + s[1] + "," + s[2] + "," + s[3]);
                    }
                    pendingReceivers.remove(key);
                }

                yml.set(path + ".receivers", temp); // Save receiver list

                removeCircuit(key); // Remove circuit from memory

                count += 1;
            }
        }
        loadedWorlds.remove(world); // World isn't loaded anymore.
        yml.save();
    }

    // Save all data
    public void Save() {
        Map<String, Integer> worldCount = new HashMap<String, Integer>();

        for (World world : plugin.getServer().getWorlds()) {
            if (yml.get(world.getName()) != null) { // World Exists
                yml.set(world.getName(), null); // Remove world circuits
            }
        }
        for (Location key : circuitLocations()) {
            Circuit currentCircuit = getCircuit(key);
            String wName = key.getWorld().getName(); // Get world name
            int count = worldCount.get(wName) == null ? 0 : worldCount.get(wName); // Get world circuit ID
            String path = wName + ".circuit_" + count; // Set base path

            yml.set(path + ".type", currentCircuit.type); // Save Type
            yml.set(path + ".sender", key.getBlockX() + "," + key.getBlockY() + "," + key.getBlockZ()); // Save sender location

            List<String> temp = new ArrayList<String>();
            for (Location loc : currentCircuit.getReceivers()) // Loop through receivers
            {
                temp.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getWorld().getName());
            }

            if (pendingReceivers.containsKey(key)) {
                for (String[] s : pendingReceivers.get(key)) {
                    temp.add(s[0] + "," + s[1] + "," + s[2] + "," + s[3]);
                }
            }

            yml.set(path + ".receivers", temp); // Save receiver list

            worldCount.put(wName, count + 1); // Increase Count
        }
        yml.save();
    }

    // Loads chunks that contain circuits (with a range around them as well).
    private void preloadChunks() {
        for (Location loc : circuitLocations()) {
            // Don't need to load sender...!
            for (Location l : getCircuit(loc).getReceivers()) {
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
                        Chunk chunk = world.getChunkAt(center.getX() + dx, center.getZ() + dz);
                        world.loadChunk(chunk);
                    }
                }
            }
        }
    }
}