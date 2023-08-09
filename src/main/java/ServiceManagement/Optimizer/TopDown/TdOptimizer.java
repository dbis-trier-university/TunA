package ServiceManagement.Optimizer.TopDown;

import Configuration.Configuration;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.Optimizer.PlanOptimizer;
import ServiceManagement.Plan.ServicePlan;
import ServiceManagement.Plan.StateChart;

import java.util.List;
import java.util.logging.Logger;

public abstract class TdOptimizer extends PlanOptimizer {
    static final Logger logger = Configuration.getInstance().getLogger();
    private static int PROCESSING_TIME = 1000;
    private static int API_OPT_TIME;
    private long qTime;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public TdOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting);
        this.qTime = qTime;

        if(this instanceof TdSingleOptimizer) API_OPT_TIME = 7; //10
        else if(this instanceof  TdListOptimizer) API_OPT_TIME = 3; //5
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

        logger.fine("Remove redundant calls and compute quality of service estimations");
        start = System.currentTimeMillis();
        ServicePlan plan = serviceStateChart.getCompletePlan();
        GapMap gapMap = plan.removeRedundantCalls();
        end = System.currentTimeMillis();
        time = end - start;
        logger.fine("Done after " + time + "ms.");
        logger.fine("Gaps: " + gapMap.size());

        int optTime = gapMap.getPlan().callSize() * API_OPT_TIME;
        long constTime = qTime + PROCESSING_TIME + optTime;

        if(constTime >= getSetting().get("Time")){
            return null;
        } else if(gapMap.getPlan().getQos().getExecTime() + constTime > getSetting().get("Time")){
            logger.fine("Optimize plan according to user settings " + super.getSetting());
            start = System.currentTimeMillis();
            gapMap = optimize(gapMap,constTime);
            end = System.currentTimeMillis();
            time = end - start;
            logger.fine("Done optimizing after " + time + "ms.: " + gapMap.getPlan().toString());
        } else {
            logger.fine("No optimization needs to be done.");
            gapMap.computeOptPlan();
        }
        System.out.println("Est. Time: " + (gapMap.getPlan().getQos().getExecTime() + constTime) / 1000.0);

        return gapMap;
    }

    abstract GapMap optimize(GapMap GapMap, long constTime);

}
