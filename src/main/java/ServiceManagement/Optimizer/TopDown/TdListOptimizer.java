package ServiceManagement.Optimizer.TopDown;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.QualityOfService;
import org.apache.jena.atlas.lib.Pair;

import java.util.LinkedList;
import java.util.List;

public class TdListOptimizer extends TdOptimizer {

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public TdListOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting,qTime);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    GapMap optimize(GapMap gapMap, long constTime){
        while(gapMap.getPlan().getQos().getExecTime() + constTime> this.getSetting().get("Time")){
            String apiName = gapMap.getMostExpensiveApiName();

            List<FuncInstance> removableInstances = identifyRemovableList(apiName,gapMap,constTime);
            gapMap.remove(apiName,getQueryResult(), removableInstances);
        }

        gapMap.computeOptPlan();

        return gapMap;
    }

    private List<FuncInstance> identifyRemovableList(String apiName, GapMap gapMap, long constTime){
        // Quality of service contains (negative) diffs.
        // The closer the quality is to 0 the less impact has the single API call
        List<Pair<FuncInstance,QualityOfService>> orderedInstances = Utils.getOrderedNamedInstances(apiName,getQueryResult(),gapMap,this.getSetting());
        double reductionTime = gapMap.getTimeMap().get(apiName) + constTime - this.getSetting().get("Time");

        // Delete all calls that are redundant
        double time = 0;
        List<FuncInstance> removableInstances = new LinkedList<>();
        for (int i = 0; i < orderedInstances.size(); i++) {
            Pair<FuncInstance,QualityOfService> pair = orderedInstances.get(i);

            if(gapMap.hasBackUp(pair.getLeft())){
                double responseTime = FunctionStore.get(pair.getLeft().getApiName()).getResponseTime();
                double timeout = 0;
                if(i < orderedInstances.size()-1) timeout = pair.getLeft().getService().getTimeout();


                time += responseTime + timeout;
                removableInstances.add(pair.getLeft());
                if(time >= reductionTime) break;
            }
        }

        // In case removing redundant calls was not enough, remove also non-redundant ones.
        if (removableInstances.size() != orderedInstances.size() && time < reductionTime) {
            for(Pair<FuncInstance,QualityOfService> pair : orderedInstances){
                if(!removableInstances.contains(pair.getLeft())){
                    double responseTime = FunctionStore.get(pair.getLeft().getApiName()).getResponseTime();
                    double timeout = pair.getLeft().getService().getTimeout();

                    time += responseTime + timeout;
                    removableInstances.add(pair.getLeft());
                    if(time >= reductionTime) break;
                }
            }
        }

        return removableInstances;
    }

}
