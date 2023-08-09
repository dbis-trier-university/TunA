package ServiceManagement.Optimizer;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;

import java.util.List;

public abstract class PlanOptimizer {
    private QueryResult queryResult;
    private UserPreferences setting;
    private List<Function> functionStore;

    public PlanOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting) {
        this.queryResult = queryResult;
        this.functionStore = functionStore;
        this.setting = setting;
    }

    public QueryResult getQueryResult() {
        return queryResult;
    }

    public UserPreferences getSetting() {
        return setting;
    }

    public List<Function> getFunctionStore() {
        return functionStore;
    }

    abstract public GapMap identifyBestPlan();
}
