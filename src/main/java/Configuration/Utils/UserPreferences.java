package Configuration.Utils;

import org.apache.jena.atlas.lib.Pair;

import java.util.LinkedList;
import java.util.List;

public class UserPreferences {
    private List<Pair<String,Double>> settingOrder;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public UserPreferences(List<Pair<String,Double>> settingOrder){
        this.settingOrder = settingOrder;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public double get(String key){
        for(Pair<String,Double> pair : this.settingOrder){
            if(pair.getLeft().equalsIgnoreCase(key)) return pair.getRight();
        }

        return -1.0;
    }

    public List<String> getOrderedPreferences(){
        List<String> orderedPreferences = new LinkedList<>();

        for(Pair<String,Double> pair : this.settingOrder){
            orderedPreferences.add(pair.getLeft());
        }

        return orderedPreferences;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder("{ ");

        for (int i = 0; i < this.settingOrder.size(); i++) {
            Pair<String,Double> pair = this.settingOrder.get(i);
            if(i == this.settingOrder.size() - 1) str.append(pair.toString()).append(" ");
            else str.append(pair.toString()).append(", ");
        }
        str.append("}");

        return str.toString();
    }
}
