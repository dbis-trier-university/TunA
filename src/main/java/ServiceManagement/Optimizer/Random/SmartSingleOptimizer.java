package ServiceManagement.Optimizer.Random;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SmartSingleOptimizer extends RdnOptimizer {

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public SmartSingleOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting, qTime);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    GapMap optimize(GapMap gapMap,long constTime){
        while(gapMap.getPlan().getQos().getExecTime() + constTime > this.getSetting().get("Time")){
            String apiName = gapMap.getMostExpensiveApiName();

            List<FuncInstance> list = new LinkedList<>();
            for(FuncInstance instance : gapMap.getPlan().toList()){
                if(instance.getApiName().equals(apiName)) list.add(instance);
            }

            Random rdn = new Random();
            int randomIndex = rdn.nextInt(list.size());
            FuncInstance randomInstance = list.get(randomIndex);

            boolean[] tried = new boolean[list.size()];
            while (!triedAll(tried)){
                randomIndex = rdn.nextInt(list.size());
                tried[randomIndex] = true;
                randomInstance = list.get(randomIndex);
                if(gapMap.hasBackUp(randomInstance)) break;
            }

            List<FuncInstance> removableInstances = new LinkedList<>();
            removableInstances.add(randomInstance);

            gapMap.remove(apiName,getQueryResult(),removableInstances);
        }

        gapMap.computeOptPlan();

        return gapMap;
    }

    private boolean triedAll(boolean[] tried){
        boolean status = true;

        for(boolean b : tried){
            status = status & b;
        }

        return status;
    }
}
