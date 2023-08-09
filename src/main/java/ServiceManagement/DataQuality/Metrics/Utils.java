package ServiceManagement.DataQuality.Metrics;

import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;

import java.util.List;

public class Utils {

    public static String createColumnKey(FuncInstance instance){
        StringBuilder colKey = new StringBuilder(instance.getRowId() + ":");

        List<LocalPath> paths = instance.getSelectedAlignments().iterator().next().getLocalRelations();
        for (int i = 0; i < paths.size(); i++) {
            colKey.append(paths.get(i).getLast());
            if(i != paths.size()-1) colKey.append(":");
        }

        return colKey.toString();
    }

    public static boolean recomputationNeeded(int id, List<FuncInstance> remInstances, FuncInstance orgInstance){
        if (remInstances == null) return false;
        if (id < 0) return true;

        List<LocalPath> paths = orgInstance.getSelectedAlignments().iterator().next().getLocalRelations();
        for(FuncInstance instance : remInstances){
            for(Alignment alignment : instance.getSelectedAlignments()){
                for(LocalPath path : alignment.getLocalRelations()){
                    if(path.isLast(paths.get(0).getLast())) return true;
                }
            }
        }

        return false;
    }

    public static boolean recomputationNeeded(int id, List<FuncInstance> remInstances, String relation){
        if (remInstances == null) return false;
        if (id < 0) return true;

        for(FuncInstance instance : remInstances){
            for(Alignment alignment : instance.getSelectedAlignments()){
                for(LocalPath path : alignment.getLocalRelations()){
                    if(path.isLast(relation)) return true;
                }
            }
        }

        return false;
    }

    public static double round(double value){
        return Math.round(value * Math.pow(10,2))/Math.pow(10,2);
    }
}
