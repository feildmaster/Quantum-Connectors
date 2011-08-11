package feildmaster.QuantumConnectors.Listeners;

import feildmaster.QuantumConnectors.QuantumConnectors;
import org.bukkit.Location;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Author: niftymonkey - https://github.com/niftymonkey
 *         feildmaster - https://github.com/feildmaster
 */
public class QuantumConnectorsWorldListener extends WorldListener
{
    private QuantumConnectors plugin;

    public QuantumConnectorsWorldListener(QuantumConnectors plugin) {
        this.plugin = plugin;
    }

    public void onWorldLoad(WorldLoadEvent event) {
        plugin.circuits.loadWorld(event.getWorld());
    }
    
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.circuits.saveWorld(event.getWorld().getName());
    }

    public void onChunkUnload(ChunkUnloadEvent event) {
        // TODO: Different way of unloading? I don't like looping so much on every chunk! -- Feildmaster
        
        if (!event.isCancelled())
            for (Location loc : plugin.circuits.circuitLocations())
                for(Location l : plugin.circuits.getCircuit(loc).getReceivers()) {
                    int circuitChunkX = loc.getBlock().getChunk().getX();
                    int chunkX = event.getChunk().getX();
                    int circuitChunkZ = loc.getBlock().getChunk().getZ();
                    int chunkZ = event.getChunk().getZ();
                    
                    // if this chunk is not inside chunk range, ignore this event and move on
                    if(Math.abs(chunkX - circuitChunkX) > plugin.getChunkUnloadRange() || Math.abs(chunkZ - circuitChunkZ) > plugin.getChunkUnloadRange())
                        continue;
                    
                    event.setCancelled(true);
                    break;
                }
    }
}
