package ServiceManagement.Optimizer.TopDown;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.QualityOfService;
import org.apache.jena.atlas.lib.Pair;

import java.util.LinkedList;
import java.util.List;

public class TdSingleOptimizer extends TdOptimizer {
    private static int API_OPT_TIME = 32;
    private long qTime;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public TdSingleOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting,qTime);
        this.qTime = qTime;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    GapMap optimize(GapMap gapMap, long constTime){
        while(gapMap.getPlan().getQos().getExecTime() + constTime > this.getSetting().get("Time")){
            String apiName = gapMap.getMostExpensiveApiName();

            List<FuncInstance> removableInstances = identifyRemovableInstance(apiName,gapMap); // TODO EXPERIMENTAL
            gapMap.remove(apiName,getQueryResult(),removableInstances);
        }

        gapMap.computeOptPlan();

        return gapMap;
    }

    private List<FuncInstance> identifyRemovableInstance(String apiName, GapMap gapMap){
        // Quality of service contains (negative) diffs.
        // The closer the quality is to 0 the less impact has the single API call
        List<Pair<FuncInstance, QualityOfService>> orderedInstances = Utils.getOrderedNamedInstances(apiName,getQueryResult(),gapMap,this.getSetting());

        List<FuncInstance> tmp = new LinkedList<>();
        for(Pair<FuncInstance,QualityOfService> pair : orderedInstances){
            if(gapMap.hasBackUp(pair.getLeft())){
                tmp.add(pair.getLeft());
                return tmp;
            }
        }

        tmp.add(orderedInstances.get(0).getLeft());
        return tmp;
    }

}
