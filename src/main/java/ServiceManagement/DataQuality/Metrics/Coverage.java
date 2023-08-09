package ServiceManagement.DataQuality.Metrics;

import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.FunctionStore;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;

public class Coverage {
    // Map<QoS-ID,Map<Row:Relation,Coverage>
    private static Map<Integer,Map<String,Double>> coverageMap = new HashMap<>();

    public static double computeCoverage(int id, List<FuncInstance> instances){
        return computeCoverage(id,instances,null);
    }

    public static double computeCoverage(int id, List<FuncInstance> instances, List<FuncInstance> remInstances){
        instances.sort(Comparator.comparing(FuncInstance::getUrl));
        double coverage = 0;

        Map<FuncInstance,Boolean> handled = new HashMap<>();
        for(FuncInstance instance : instances){
            handled.put(instance,false);
        }

        int counter = 0;
        for (int i = 0; i < instances.size(); i++) {
            List<FuncInstance> backupInstances = new LinkedList<>(getBackUp(i,instances,handled));

            if(backupInstances.size() > 1) {
                coverage += computeUnion(id,instances.get(i),backupInstances,remInstances);
                counter++;
            } else if (backupInstances.size() == 1){
                coverage += FunctionStore.get(instances.get(i).getApiName()).getResponseProbability();
                counter++;
            }
        }
        coverage = coverage / counter;

        return Utils.round(coverage);
    }

    public static double computeUnion(int id, FuncInstance orgInstance,
                                      List<FuncInstance> instances, List<FuncInstance> remInstances)
    {
        double value = 0;

        String colKey = Utils.createColumnKey(orgInstance);
        if(!Utils.recomputationNeeded(id,remInstances,orgInstance)
                && coverageMap.containsKey(id)
                && coverageMap.get(id).containsKey(colKey))
        {
            value = coverageMap.get(id).get(colKey);
        } else {
            for (int k = 1; k < instances.size();k++) {
                double tmp = Math.pow(-1,k+1) * intersect(instances, k);
                value += tmp;
            }

            if(id > 0){
                Map<String,Double> tmp;
                if(coverageMap.containsKey(id)) tmp = coverageMap.get(id);
                else tmp = new HashMap<>();

                tmp.put(colKey,value);
                coverageMap.put(id,tmp);
            }
        }

        return value;
    }

    public static double intersect(List<FuncInstance> instances, int k){
        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(instances.size()-1,k);

        double value = 0;

        while (iterator.hasNext()){
            double pk = 1;
            final int[] combination = iterator.next();

            for(int i : combination){
                pk *= instances.get(i).getResponseProbability();
            }

            value += pk;
        }

        return value;
    }

    private static Set<FuncInstance> getBackUp(int index,
                                               List<FuncInstance> instances,
                                               Map<FuncInstance,Boolean> handled)
    {
        Set<FuncInstance> backupInstances = new HashSet<>();
        FuncInstance instance = instances.get(index);

        if(!handled.get(instance)){
            backupInstances.add(instance);
            handled.put(instance,true);

            for (int i = index+1; i < instances.size(); i++) {
                FuncInstance tmpInstance = instances.get(i);

                if(!handled.get(tmpInstance) && instance.getRowId() == tmpInstance.getRowId()){
                    if(overlappingSelect(instance,tmpInstance)){
                        backupInstances.add(tmpInstance);
                        handled.put(tmpInstance,true);
                    }
                }
            }
        }

        return backupInstances;
    }

    private static boolean overlappingSelect(FuncInstance instance, FuncInstance tmpInstance){
        for(Alignment a1: instance.getAlignments()) {
            for (Alignment a2 : tmpInstance.getAlignments()) {
                if (Alignment.overlaps(a1, a2)) return true;
            }
        }

        return false;
    }
}
