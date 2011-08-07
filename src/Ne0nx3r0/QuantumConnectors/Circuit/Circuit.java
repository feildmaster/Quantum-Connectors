package Ne0nx3r0.QuantumConnectors.Circuit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

public class Circuit{
    public int type;
    private List<Location> receivers = new ArrayList<Location>();
    
    public Circuit(int i, Location loc){
        type = i;
        receivers.add(loc);
    }
    public Circuit(int i, List<Location> list) {
        type = i;
        receivers = list;
    }
    
    public void setReceivers(List<Location> list) {
        receivers = list;
    }
    public void addReceiver(Location loc) {
        receivers.add(loc);
    }
    public void delReceiver(Location loc) {
        receivers.remove(loc);
    }
    public Boolean getReceiver(Location loc) {
        return receivers.contains(loc);
    }
    public List<Location> getReceivers() {
        return receivers;
    }
}
