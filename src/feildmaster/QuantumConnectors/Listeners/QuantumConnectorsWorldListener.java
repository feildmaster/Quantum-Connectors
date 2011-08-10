package Ne0nx3r0.QuantumConnectors.Listeners;

import Ne0nx3r0.QuantumConnectors.QuantumConnectors;
import org.bukkit.Location;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Author: niftymonkey - https://github.com/niftymonkey
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
        // THIS LISTENER TAKES UP TOO MUCH MEMORY!!!
        // only bother to do this if the event hasn't already been cancelled
        if (!event.isCancelled())
            for (Location loc : plugin.circuits.circuitLocations())
                for(Location l : plugin.circuits.getCircuit(loc).getReceivers()) {
                    // determine whether or not this chunk is within the range of the circuit
                    // (based on the chunk's X and Z ... not the block's x and z)

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
