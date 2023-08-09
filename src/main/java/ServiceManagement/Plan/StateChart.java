package ServiceManagement.Plan;

import Configuration.Configuration;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.MissingResult;
import QueryManagement.Query.Result.QueryResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StateChart {
    private static final Logger logger = Configuration.getInstance().getLogger();
    Map<MissingResult, List<FuncInstance>> serviceStateChart;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public StateChart(Map<MissingResult, List<FuncInstance>> serviceStateChart) {
        this.serviceStateChart = serviceStateChart;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public static StateChart createServiceStateChart(QueryResult queryResult, List<Function> functionStore){
        logger.fine("Start creating a state chart.");
        Map<MissingResult,List<FuncInstance>> stateChartMap = new HashMap<>();

        logger.finer("Identify gaps in the result table of the query.");
        List<MissingResult> missingResults = queryResult.getMissingResultEntryMap();

        logger.finer("Create API instantiations for all possible APIs");
        for(MissingResult result : missingResults){
            List<FuncInstance> instances = FuncInstance.createFunctionInstances(result,functionStore);
            if(instances.size() > 0) stateChartMap.put(result,instances);
        }

        logger.finer("Remove all gaps with no service that can be called.");
        stateChartMap.values().removeIf(value -> value == null || value.size() < 1);

        return new StateChart(stateChartMap);
    }

    public Map<MissingResult, List<FuncInstance>> getServiceStateChart() {
        return serviceStateChart;
    }

    public ServicePlan getCompletePlan(){
        List<FuncInstance> list = new LinkedList<>();
        for(Map.Entry<MissingResult,List<FuncInstance>> entry : this.serviceStateChart.entrySet()){
            list.addAll(entry.getValue());
        }

        return new ServicePlan(list);
    }

    // TODO experimental
    public ServicePlan getTruncatedPlan(UserPreferences setting){
        List<FuncInstance> list = new LinkedList<>();
        int reliability = (int) setting.get("Reliability") * 100;
        int minInstances = 10;
        double trust = (((double) reliability)/minInstances);
        do {
            reliability = reliability % 2 == 0 ? (reliability/2) : ((reliability/2) + 1);
            minInstances = minInstances/2;
        } while (trust >= setting.get("Reliability"));

        for(Map.Entry<MissingResult,List<FuncInstance>> entry : this.serviceStateChart.entrySet()){
            List<FuncInstance> tmpList = entry.getValue();
            tmpList.sort((i1,i2) -> {
               int rt1 = FunctionStore.get(i1.getApiName()).getResponseTime();
               int rt2 = FunctionStore.get(i2.getApiName()).getResponseTime();
               return Integer.compare(rt1,rt2);
            });

            for (int i = 0; i < Math.min(minInstances, tmpList.size()); i++) {
                list.add(tmpList.get(i));
            }
        }

        return new ServicePlan(list);
    }

    public int getNumberOfLayers(){
        return this.serviceStateChart.size();
    }

    public int getMaxNumberOfPlans(){
        int number = 1;

        for(Map.Entry<MissingResult,List<FuncInstance>> entry : this.serviceStateChart.entrySet()){
            number *= entry.getValue().size();
        }

        return number;
    }

}
