package Ne0nx3r0.QuantumConnectors;

import org.bukkit.Location;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;

/**
 * Author: niftymonkey - https://github.com/niftymonkey
 */
public class QuantumConnectorsWorldListener extends WorldListener
{
    private QuantumConnectors plugin;

    public QuantumConnectorsWorldListener(QuantumConnectors plugin)
    {
        this.plugin = plugin;
    }

    public void onChunkUnload(ChunkUnloadEvent event)
    {
        // only bother to do this if the event hasn't already been cancelled
        if (!event.isCancelled())
        {
            // iterate over our circuit locations
            for (Location loc : CircuitManager.getCircuitLocations())
            {
                // determine whether or not this chunk is within the range of the circuit
                // (based on the chunk's X and Z ... not the block's x and z)

                // if this chunk is not inside that range, ignore this event and move on

                int circuitChunkX = loc.getBlock().getChunk().getX();
                int chunkX = event.getChunk().getX();
                if(Math.abs(chunkX - circuitChunkX) > plugin.getChunkUnloadRange())
                {
                    continue;
                }

                int circuitChunkZ = loc.getBlock().getChunk().getZ();
                int chunkZ = event.getChunk().getZ();
                if(Math.abs(chunkZ - circuitChunkZ) > plugin.getChunkUnloadRange())
                {
                    continue;
                }

                // if we made it to here, that means that we ARE inside the range/matrix of chunks that surround a circuit
                // so we're going to cancel the event so that this chunk doesn't get unloaded

                plugin.getLogger().finer("[QuantumConnectors] - Chunk contains or surrounds circuit node, not unloading.");
                event.setCancelled(true);

                return;
            }
        }
    }
}
