package ServiceManagement.Optimizer.Random;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ListOptimizer extends RdnOptimizer {

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public ListOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting,qTime);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    GapMap optimize(GapMap gapMap, long constTime){
        while(gapMap.getPlan().getQos().getExecTime() + constTime > this.getSetting().get("Time")){
            String apiName = gapMap.getMostExpensiveApiName();

            List<FuncInstance> removableInstances = identifyRemovableList(apiName,gapMap,constTime);
            gapMap.remove(apiName, getQueryResult(), removableInstances);
        }

        gapMap.computeOptPlan();

        return gapMap;
    }

    private List<FuncInstance> identifyRemovableList(String apiName, GapMap gapMap, long constTime){
        double reductionTime = gapMap.getTimeMap().get(apiName) + constTime - this.getSetting().get("Time");

        List<FuncInstance> list = new LinkedList<>();
        for(FuncInstance instance : gapMap.getPlan().toList()){
            if(instance.getApiName().equals(apiName)) list.add(instance);
        }

        double time = 0;
        List<FuncInstance> removableInstances = new LinkedList<>();
        while (time < reductionTime){
            Random rdn = new Random();
            int randomIndex = rdn.nextInt(list.size());

            double responseTime = FunctionStore.get(list.get(randomIndex).getApiName()).getResponseTime();
            double timeout = list.get(randomIndex).getService().getTimeout();

            time += responseTime + timeout;
            removableInstances.add(list.get(randomIndex));
        }

        return removableInstances;
    }

}
