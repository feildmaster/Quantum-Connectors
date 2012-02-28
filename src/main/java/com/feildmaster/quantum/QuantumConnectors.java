package com.feildmaster.quantum;

import com.feildmaster.lib.configuration.PluginWrapper;
import com.feildmaster.quantum.circuit.*;
import com.feildmaster.quantum.listeners.*;
import java.io.File;
import org.bukkit.plugin.PluginManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import java.util.HashMap;

public class QuantumConnectors extends PluginWrapper {
    private final QuantumConnectorsBlockListener blockListener = new QuantumConnectorsBlockListener(this);
    private final QuantumConnectorsPlayerListener playerListener = new QuantumConnectorsPlayerListener(this);
    private final QuantumConnectorsWorldListener worldListener = new QuantumConnectorsWorldListener(this);
    public static Map<String, Integer> circuitTypes = new HashMap<String, Integer>();
    public static CircuitManager circuits;
    //Configurables
    private int MAX_CHAIN_LINKS = 3;
    private int AUTOSAVE_INTERVAL = 10;//specified here in minutes
    private int CHUNK_UNLOAD_RANGE = 0; //number of chunks surrounding the circuit to keep around when unloading chunks

    // Public "gets"
    public void msg(Player player, String sMessage) {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "[QC] " + ChatColor.WHITE + sMessage);
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
        pm.registerEvents(blockListener, this);
        pm.registerEvents(playerListener, this);
        pm.registerEvents(worldListener, this);

        //Setup circuits
        for (Type t : Type.values()) {
            circuitTypes.put(t.name, t.id);
        }

        //Configuration
        loadConfig();

        //Scheduled saves
        getServer().getScheduler().scheduleSyncRepeatingTask(this, autosaveCircuits, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL);

        //Setup circuit manager
        circuits = new CircuitManager(new File(this.getDataFolder(), "circuits.yml"), this);
    }

    public void onDisable() {
        circuits.Save();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            this.getLogger().info("This command has to be called by a player");
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
            msg(p, "To create a quantum circuit, use /qc <circuit>; and click   on a sender and then a receiver with redstone.");

            String s = "";
            for (String sKey : circuitTypes.keySet()) {
                s += sKey + ", ";
            }

            msg(p, ChatColor.YELLOW + "Available circuits: " + ChatColor.WHITE + s.substring(0, s.length() - 2));
        } else if (args[0].equalsIgnoreCase("cancel")) {
            if (playerListener.pendingCircuits.containsKey(p)) {
                playerListener.pendingCircuits.remove(p);
                playerListener.pendingSenders.remove(p);

                msg(p, "Pending circuit removed!");
            } else {
                msg(p, "No pending circuits");
            }
        } else if (circuitTypes.containsKey(args[0])) {
            if (p.hasPermission("QuantumConnectors.create." + args[0].toLowerCase())) {
                playerListener.pendingCircuits.put(p, circuitTypes.get(args[0].toLowerCase()));
                msg(p, "Circuit is ready to be created!");
            } else {
                msg(p, ChatColor.RED + "You don't have permission to create the " + args[0] + " circuit!");
            }
        } else {
            msg(p, "Invalid circuit specified!");
        }

        return true;
    }

    private void loadConfig() {
        if (getConfig().needsUpdate()) {
            getConfig().saveDefaults();
        }

        MAX_CHAIN_LINKS = getConfig().getInt("max_chain_links");
        AUTOSAVE_INTERVAL = getConfig().getInt("autosave_minutes") * 60 * 20;//convert to minutes
        CHUNK_UNLOAD_RANGE = getConfig().getInt("chunk_unload_range");
    }

    public void activateCircuit(Location lSender, int current) {
        circuits.activateCircuit(lSender, current, 0);
    }

    public enum Type {
        QUANTUM(0),
        ON(1),
        OFF(2),
        TOGGLE(3),
        REVERSE(4),
        RANDOM(5);

        int id;
        String name;

        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        Type(int id) {
            this.id = id;
            this.name = this.name().toLowerCase();
        }

        public int getId() {
            return id;
        }
    }

    //Scheduled save mechanism
    private Runnable autosaveCircuits = new Runnable() {
        public void run() {
            circuits.Save();
        }
    };
}