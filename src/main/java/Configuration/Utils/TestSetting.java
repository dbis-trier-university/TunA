package Configuration.Utils;

import ServiceManagement.Optimizer.Optimizer;

public class TestSetting {
    private String label, queryPath, resultPath, dbLabel, functionStore;
    private Optimizer op;
    private UserPreferences setting;

    public TestSetting(String label,
                       String queryPath, String resultPath,
                       String dbLabel, String functionStore,
                       String processor, UserPreferences setting)
    {
        this.label = label;
        this.queryPath = queryPath;
        this.resultPath = resultPath;
        this.dbLabel = dbLabel;
        this.functionStore = functionStore;
        this.setting = setting;

        switch (processor){
            case "TD_SINGLE" -> op = Optimizer.TD_SINGLE;
            case "TD_LIST" -> op = Optimizer.TD_LIST;
            case "RDN_SINGLE" -> op = Optimizer.RDN_SINGLE;
            case "RDN_LIST" -> op = Optimizer.RDN_LIST;
            default -> op = Optimizer.TD_LIST;
        }
    }

    public String getLabel() {
        return label;
    }

    public String getQueryPath() {
        return queryPath;
    }

    public String getResultPath() {
        return resultPath;
    }

    public String getDbLabel() {
        return dbLabel;
    }

    public String getFunctionStore() {
        return functionStore;
    }

    public Optimizer getProcessor() {
        return op;
    }

    public UserPreferences getSetting() {
        return setting;
    }
}
