package Ne0nx3r0.QuantumConnectors.Listeners;

import Ne0nx3r0.QuantumConnectors.QuantumConnectors;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class QuantumConnectorsPlayerListener extends PlayerListener {
    private final QuantumConnectors plugin;

    public static Map<Player,Integer> pendingCircuits;
    public static Map<Player,Location> pendingSenders;

    public QuantumConnectorsPlayerListener(QuantumConnectors instance){
        this.plugin = instance;
        
        pendingCircuits = new HashMap<Player,Integer>();
        pendingSenders = new HashMap<Player,Location>();
    }

    public void onPlayerInteract(PlayerInteractEvent event){
        if(event.isCancelled()) return;

        //holding redstone, clicked a block, and has a pending circuit from /qc
        if(event.getItem() != null
        && event.getItem().getType() == Material.REDSTONE
        && event.getClickedBlock() != null
        && pendingCircuits.containsKey(event.getPlayer())){
            Player player = event.getPlayer();
            Block block = event.getClickedBlock();

            //setting up a sender
            if(!pendingSenders.containsKey(player)){
                if(plugin.circuits.isValidSender(block)){
                    if(plugin.circuits.circuitExists(block.getLocation())){
                        plugin.msg(player,ChatColor.YELLOW+"A circuit already exists here!");
                    } else {
                        pendingSenders.put(player,event.getClickedBlock().getLocation());
                        plugin.msg(player,"Sender saved!");
                    }
                }else if(plugin.circuits.circuitExists(block.getLocation())){//remove a possibly leftover circuit
                    plugin.circuits.removeCircuit(block.getLocation());
                    plugin.msg(player,ChatColor.YELLOW+"An old circuit was here, now removed - try again!");
                }else {
                    plugin.msg(player,ChatColor.RED+"Invalid sender!");
                    plugin.msg(player,ChatColor.YELLOW+"Senders: "+ChatColor.WHITE+plugin.circuits.getValidSendersString());
                }
            }else{//setting up a receiver
                if(pendingSenders.get(player).toString().equals(event.getClickedBlock().getLocation().toString())) {
                    plugin.msg(player, ChatColor.YELLOW+"Receiver can not be the same as sender!");
                }else if (plugin.circuits.isValidReceiver(block)) {
                    plugin.circuits.addCircuit(
                        pendingSenders.get(player),
                        event.getClickedBlock().getLocation(),
                        pendingCircuits.get(player)
                    );

                    if(event.getClickedBlock().getType() == Material.WOODEN_DOOR){
                        int iData = (int) event.getClickedBlock().getData();
                        Block bOtherPiece = block.getRelative((iData&0x08) == 0x08?BlockFace.DOWN:BlockFace.UP);

                        plugin.circuits.addCircuit(
                            bOtherPiece.getLocation(),
                            event.getClickedBlock().getLocation(),
                            pendingCircuits.get(player)
                        );
                    }

                    pendingSenders.remove(player);
                    pendingCircuits.remove(player);

                    plugin.msg(player,"Quantum Connector created!");
                }else if(plugin.circuits.circuitExists(block.getLocation())){//remove a possibly leftover circuit
                    plugin.circuits.removeCircuit(block.getLocation());
                    plugin.msg(player,ChatColor.YELLOW+"An old circuit was here, now removed - try again!");
                }else{
                    plugin.msg(player,ChatColor.RED+"Invalid receiver!");
                    plugin.msg(player,ChatColor.YELLOW+"Receivers: "+ChatColor.WHITE+plugin.circuits.getValidReceiversString());
                }
            }
            event.setCancelled(true); // Cancel any toggles or breaks.
        }
        //trigger for using wood/trap doors as senders
        else if(event.getClickedBlock() != null
        && plugin.circuits.circuitExists(event.getClickedBlock().getLocation())){
            Block block = event.getClickedBlock();

            if(block.getType() == Material.WOODEN_DOOR || block.getType() == Material.TRAP_DOOR){
                plugin.activateCircuit(event.getClickedBlock().getLocation(),plugin.circuits.getBlockCurrent(block));
            }
        }
    }
}