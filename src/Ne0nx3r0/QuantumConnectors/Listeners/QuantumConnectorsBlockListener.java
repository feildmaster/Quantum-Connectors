package Ne0nx3r0.QuantumConnectors.Listeners;

import Ne0nx3r0.QuantumConnectors.QuantumConnectors;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class QuantumConnectorsBlockListener extends BlockListener{
    private final QuantumConnectors plugin;

    public QuantumConnectorsBlockListener(final QuantumConnectors plugin){
        this.plugin = plugin;
    }

    public void onBlockRedstoneChange(BlockRedstoneEvent event){
        if(plugin.circuits.circuitExists(event.getBlock().getLocation())){
            plugin.activateCircuit(event.getBlock().getLocation(),event.getNewCurrent());
        }
    }

    public void onBlockBreak(BlockBreakEvent event){
        if(plugin.circuits.circuitExists(event.getBlock().getLocation())){
            plugin.circuits.removeCircuit(event.getBlock().getLocation());
        }
    }
}