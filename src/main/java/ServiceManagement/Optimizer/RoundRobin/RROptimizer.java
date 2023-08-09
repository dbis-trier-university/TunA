package ServiceManagement.Optimizer.RoundRobin;

import Configuration.Configuration;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.MissingResult;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.DataQualityFactory;
import ServiceManagement.DataQuality.Metrics.Coverage2;
import ServiceManagement.DataQuality.Metrics.Reliability;
import ServiceManagement.DataQuality.QualityOfService;
import ServiceManagement.Optimizer.PlanOptimizer;
import ServiceManagement.Plan.ServicePlan;
import ServiceManagement.Plan.StateChart;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;
import java.util.logging.Logger;

public class RROptimizer extends PlanOptimizer {
    static final Logger logger = Configuration.getInstance().getLogger();
    private static final int PROCESSING_TIME = 1500;
    private final long qTime;
    Map<String,Integer> cMap;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public RROptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting, long qTime) {
        super(queryResult, functionStore,setting);
        this.cMap = new HashMap<>();
        this.qTime = qTime;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    @Override
    public GapMap identifyBestPlan() {
        logger.fine("Create state chart");
        long start = System.currentTimeMillis();
        StateChart stateChart = StateChart.createServiceStateChart(super.getQueryResult(),super.getFunctionStore());
        long end = System.currentTimeMillis();
        long time = end - start;
        logger.fine("Done after " + time + "ms.");

        double orgCoverage = ((double) getQueryResult().solCounter) / getQueryResult().getLocalSolutions().size();
        if(orgCoverage < getSetting().get("Coverage")){
            List<FuncInstance> list = ensureCoverage(stateChart);

            end = System.currentTimeMillis();
            time = end - start;

            if(list.size() > 0){
                GapMap gapMap = improveReliability(stateChart,list);
                gapMap.computeOptPlan();

                System.out.println("Est. Time: " + (gapMap.getPlan().getQos().getExecTime()+ qTime + PROCESSING_TIME) / 1000.0);
                System.out.println("Crawl Time: " + (gapMap.getPlan().getQos().getExecTime()) / 1000.0);
                System.out.println("Est. Coverage: " + gapMap.getPlan().getQos().getCoverage());
                System.out.println("Est. Reliability: " + gapMap.getPlan().getQos().getReliability());

                return gapMap;
            }
        }

        return null;
    }

    private List<FuncInstance> ensureCoverage(StateChart stateChart){
        Map<MissingResult,List<FuncInstance>> logMap = new HashMap<>();
        List<FuncInstance> list = new LinkedList<>();
        Set<String> keys = new HashSet<>();

        double orgCoverage = ((double) getQueryResult().solCounter) / getQueryResult().getLocalSolutions().size();
        double coverage = orgCoverage;

        for(Map.Entry<MissingResult,List<FuncInstance>> entry : stateChart.getServiceStateChart().entrySet()){
            boolean added = false;
            for(FuncInstance instance : entry.getValue()){
                if(!cMap.containsKey(instance.getApiName()) && !keys.contains(instance.getPreConditionValue())) {
                    cMap.put(instance.getApiName(),1);
                    list.add(instance);

                    List<FuncInstance> tmp = new LinkedList<>();
                    if(logMap.containsKey(entry.getKey())) tmp = logMap.get(entry.getKey());
                    tmp.add(instance);
                    logMap.put(entry.getKey(),tmp);

                    keys.add(instance.getPreConditionValue());
                    added = true;
                    break;
                }
            }

            if(!added){
                FuncInstance instance = searchInstance(cMap, entry.getValue(),list);

                int tmpCounter = cMap.get(Objects.requireNonNull(instance).getApiName()) + 1;
                cMap.put(instance.getApiName(),tmpCounter);
                list.add(instance);

                List<FuncInstance> tmp = new LinkedList<>();
                if(logMap.containsKey(entry.getKey())) tmp = logMap.get(entry.getKey());
                tmp.add(instance);
                logMap.put(entry.getKey(),tmp);

            }

            double tmpCoverage = Coverage2.computeCoverage(logMap,getQueryResult().noSolCounter);
            double coverageGrowth = ( (1-orgCoverage) * tmpCoverage);
            coverage = orgCoverage + coverageGrowth;

            if(coverage > getSetting().get("Coverage") || getExecTime(cMap) + qTime + PROCESSING_TIME > getSetting().get("Time")){
                return list;
            }
        }

        while ( callsLeft(stateChart,logMap)
            && getExecTime(cMap) + qTime + PROCESSING_TIME < getSetting().get("Time")
            && coverage < getSetting().get("Coverage"))
        {
            for(Map.Entry<MissingResult,List<FuncInstance>> entry : logMap.entrySet()){
                double gapCoverage = Coverage2.computeList(entry.getValue().toArray());
                List<FuncInstance> possFunctions = stateChart.getServiceStateChart().get(entry.getKey());
                FuncInstance selInstance = searchInstance(cMap, possFunctions,list);

                if (logMap.get(entry.getKey()).size() < stateChart.getServiceStateChart().get(entry.getKey()).size()
                        &&  gapCoverage < getSetting().get("Coverage")) // Check if the instances for the  individual GAP provides enough coverage
                {
                    list.add(selInstance);
                    List<FuncInstance> tmp = new LinkedList<>();
                    if(logMap.containsKey(entry.getKey())) tmp = logMap.get(entry.getKey());
                    tmp.add(selInstance);
                    logMap.put(entry.getKey(),tmp);

                    int tmpCounter = cMap.get(Objects.requireNonNull(selInstance).getApiName()) + 1;
                    cMap.put(selInstance.getApiName(),tmpCounter);

                    double tmpCoverage = Coverage2.computeCoverage(logMap,getQueryResult().noSolCounter);
                    double coverageGrowth = ( (1-orgCoverage) * tmpCoverage);
                    coverage = orgCoverage + coverageGrowth;
                }
            }
        }

        return list;
    }

    private GapMap improveReliability(StateChart stateChart, List<FuncInstance> instances){
        ServicePlan cPlan = stateChart.getCompletePlan();
        GapMap completeMap = new GapMap(cPlan);

        ServicePlan plan = new ServicePlan(instances);
        GapMap gapMap = new GapMap(plan);
        QualityOfService qos = DataQualityFactory.createFast(plan.getId(),getQueryResult(),gapMap,null);
        plan.setQos(qos);

        while(getExecTime(this.cMap) + qTime + PROCESSING_TIME < getSetting().get("Time") && qos.getReliability() < getSetting().get("Reliability")){
            for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
                double rel = Reliability.computeReliability(plan.getId(), entry.getValue());

                if (rel < getSetting().get("Reliability")){
                    List<FuncInstance> possFunctions = completeMap.getGapMap().get(entry.getKey());
                    FuncInstance selInstance = searchInstance(cMap, possFunctions,instances);

                    instances.add(selInstance);
                    plan = new ServicePlan(instances);
                    gapMap = new GapMap(plan);
                    qos = DataQualityFactory.createFast(plan.getId(),getQueryResult(),gapMap,null);
                    plan.setQos(qos);

                    int tmpCounter = cMap.get(Objects.requireNonNull(selInstance).getApiName()) + 1;
                    cMap.put(selInstance.getApiName(),tmpCounter);
                }

                if(qos.getReliability() >= getSetting().get("Reliability")) return gapMap;
            }
        }

        return gapMap;
    }


    private boolean callsLeft(StateChart stateChart, Map<MissingResult,List<FuncInstance>> map){
        for(Map.Entry<MissingResult,List<FuncInstance>> entry : map.entrySet()){
            double gapCoverage = Coverage2.computeList(entry.getValue().toArray());

            if(gapCoverage < getSetting().get("Coverage") && entry.getValue().size() < stateChart.getServiceStateChart().get(entry.getKey()).size())
                return true;
        }

        return false;
    }

    private static FuncInstance searchInstance(Map<String,Integer> cMap, List<FuncInstance> instances, List<FuncInstance> calls){
        List<Map.Entry<String, Integer>> list = new LinkedList<>(cMap.entrySet());
        list.sort(Map.Entry.comparingByValue());

        for(Map.Entry<String, Integer> entry : list){
            for(FuncInstance instance : instances){
                if(entry.getKey().equals(instance.getApiName()) && !calls.contains(instance)) {
                    return instance;
                }
            }
        }

        return null;
    }

    private static int getExecTime(Map<String,Integer> cMap){
        Map<String,Integer> timeMap = new HashMap<>();
        for (Map.Entry<String,Integer> entry : cMap.entrySet()) {
            int time = entry.getValue() * FunctionStore.get(entry.getKey()).getResponseTime() + (entry.getValue()-1) * 500;
            timeMap.put(entry.getKey(),time);
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<>(timeMap.entrySet());
        list.sort(Map.Entry.comparingByValue());

        return list.get(list.size()-1).getValue();
    }

}
