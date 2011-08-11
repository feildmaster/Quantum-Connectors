package feildmaster.QuantumConnectors.Listeners;

import feildmaster.QuantumConnectors.QuantumConnectors;
import org.bukkit.Location;
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

    public void onBlockBreak(BlockBreakEvent event) {
        Location l = event.getBlock().getLocation();
        if(plugin.circuits.circuitExists(l)) // Breaking Sender
            plugin.circuits.removeCircuit(l);
        else if (plugin.circuits.receiverExists(l))// Breaking receiver
            plugin.circuits.removeReceiver(l);
    }
}