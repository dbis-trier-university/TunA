package ServiceManagement.Optimizer.Random;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SingleOptimizer extends RdnOptimizer {

    // *****************************************************************************************************************
    // *****************************************************************************************************************

    public SingleOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting,qTime);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    GapMap optimize(GapMap gapMap, long constTime){
        while(gapMap.getPlan().getQos().getExecTime() + constTime > this.getSetting().get("Time")){
            String apiName = gapMap.getMostExpensiveApiName();

            List<FuncInstance> list = new LinkedList<>();
            for(FuncInstance instance : gapMap.getPlan().toList()){
                if(instance.getApiName().equals(apiName)) list.add(instance);
            }

            Random rdn = new Random();
            int randomIndex = rdn.nextInt(list.size());
            FuncInstance randomInstance = list.get(randomIndex);

            List<FuncInstance> removableInstances = new LinkedList<>();
            removableInstances.add(randomInstance);

            gapMap.remove(apiName,getQueryResult(),removableInstances); // TODO replace 0 by number of real gaps
        }

        gapMap.computeOptPlan();

        return gapMap;
    }
}
