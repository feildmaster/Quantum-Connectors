package com.feildmaster.quantum.listeners;

import com.feildmaster.quantum.QuantumConnectors;
import org.bukkit.Location;
import org.bukkit.event.*;
import org.bukkit.event.block.*;

public class QuantumConnectorsBlockListener implements Listener {
    private final QuantumConnectors plugin;
    public static String string;

    public QuantumConnectorsBlockListener(final QuantumConnectors plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (plugin.circuits.circuitExists(event.getBlock().getLocation())) {
            plugin.activateCircuit(event.getBlock().getLocation(), event.getNewCurrent());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location l = event.getBlock().getLocation();
        if (plugin.circuits.circuitExists(l)) { // Breaking Sender
            plugin.circuits.removeCircuit(l);
        } else if (plugin.circuits.receiverExists(l)) { // Breaking receiver
            plugin.circuits.removeReceiver(l);
        }
    }
}