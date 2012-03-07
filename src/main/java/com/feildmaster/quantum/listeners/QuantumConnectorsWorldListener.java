package com.feildmaster.quantum.listeners;

import com.feildmaster.quantum.QuantumConnectors;
import org.bukkit.Location;
import org.bukkit.event.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Author: niftymonkey - https://github.com/niftymonkey
 *         feildmaster - https://github.com/feildmaster
 */
public class QuantumConnectorsWorldListener implements Listener {
    private QuantumConnectors plugin;

    public QuantumConnectorsWorldListener(QuantumConnectors plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        QuantumConnectors.circuits.loadWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        QuantumConnectors.circuits.saveWorld(event.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // TODO: Different way of unloading? I don't like looping so much on every chunk! -- Feildmaster
        for (Location loc : QuantumConnectors.circuits.circuitLocations()) {
            for (Location l : QuantumConnectors.circuits.getCircuit(loc).getReceivers()) {
                int circuitChunkX = loc.getBlock().getChunk().getX();
                int chunkX = event.getChunk().getX();
                int circuitChunkZ = loc.getBlock().getChunk().getZ();
                int chunkZ = event.getChunk().getZ();

                // if this chunk is not inside chunk range, ignore this event and move on
                if (Math.abs(chunkX - circuitChunkX) > plugin.getChunkUnloadRange() || Math.abs(chunkZ - circuitChunkZ) > plugin.getChunkUnloadRange()) {
                    continue;
                }

                event.setCancelled(true);
                break;
            }
        }
    }
}
