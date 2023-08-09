package ServiceManagement.Optimizer.Angie;

import Configuration.Configuration;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.MissingResult;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.Optimizer.PlanOptimizer;
import ServiceManagement.Plan.ServicePlan;
import ServiceManagement.Plan.StateChart;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AngieOptimizer extends PlanOptimizer {
    static final Logger logger = Configuration.getInstance().getLogger();

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public AngieOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    @Override
    public GapMap identifyBestPlan() {
        logger.fine("Create state chart");
        long start = System.currentTimeMillis();
        StateChart serviceStateChart = StateChart.createServiceStateChart(super.getQueryResult(),super.getFunctionStore());
        long end = System.currentTimeMillis();
        long time = end - start;
        logger.fine("Done after " + time + "ms.");
        logger.finer("Number of layers: " + serviceStateChart.getNumberOfLayers());
        logger.finer("Maximum number of plans: " + serviceStateChart.getMaxNumberOfPlans());

        return createAngieGapMap(serviceStateChart);
    }

    public GapMap createAngieGapMap(StateChart stateChart){
        List<List<FuncInstance>> planList = new LinkedList<>();

        int counter = 0;
        for(Map.Entry<MissingResult,List<FuncInstance>> entry : stateChart.getServiceStateChart().entrySet()){
            List<FuncInstance> sortedList = entry.getValue();
            sortedList.sort(new AngieComparator());
            planList.add(sortedList);
            counter += sortedList.size();
        }

        List<FuncInstance> instances = new LinkedList<>();
        for (int i = 0;instances.size() < counter; i++) {
            for(List<FuncInstance> list : planList){
                if(i < list.size()) instances.add(list.get(i));
            }
        }

        ServicePlan servicePlan = new ServicePlan(instances);
        GapMap gapMap = new GapMap(servicePlan);
        gapMap.computeOptPlan();

        return gapMap;
    }
}
