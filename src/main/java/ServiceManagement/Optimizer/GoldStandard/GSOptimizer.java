package ServiceManagement.Optimizer.GoldStandard;

import Configuration.Configuration;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.MissingResult;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.DataQualityFactory;
import ServiceManagement.DataQuality.Metrics.Coverage2;
import ServiceManagement.DataQuality.Metrics.ExecutionTime;
import ServiceManagement.DataQuality.Metrics.Reliability;
import ServiceManagement.DataQuality.QualityOfService;
import ServiceManagement.Optimizer.PlanOptimizer;
import ServiceManagement.Plan.ServicePlan;
import ServiceManagement.Plan.StateChart;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class GSOptimizer extends PlanOptimizer {
    static final Logger logger = Configuration.getInstance().getLogger();
    private List<FuncInstance> minPlan = null;
    private QualityOfService minQoS = null;

    public GSOptimizer(QueryResult queryResult, List<Function> functionStore, UserPreferences setting) {
        super(queryResult, functionStore, setting);
    }

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
            logger.fine("Compute pareto solutions");
            start = System.currentTimeMillis();
            List<List<FuncInstance>> combinations = collectAllCombinations(stateChart);
            List<GapMap> paretoSolutions = calcParetoSolutions(combinations);
            GapMap gapMap = selectParetoSolution(paretoSolutions);
            end = System.currentTimeMillis();
            time = end - start;
            logger.fine("Done after " + time + "ms.");

            System.out.println("Crawl Time: " + (gapMap.getPlan().getQos().getExecTime()) / 1000.0);
            System.out.println("Est. Coverage: " + gapMap.getPlan().getQos().getCoverage());
            System.out.println("Est. Reliability: " + gapMap.getPlan().getQos().getReliability());

            writePlansToDisk(paretoSolutions);
            gapMap.computeOptPlan();
            return gapMap;
        }

        return null;
    }

    private List<List<FuncInstance>> collectAllCombinations(StateChart stateChart){
        List<Set<Set<FuncInstance>>> result = new LinkedList<>();

        List<List<FuncInstance>> gapList = new LinkedList<>();
        for(Map.Entry<MissingResult,List<FuncInstance>> entry : stateChart.getServiceStateChart().entrySet()){
            gapList.add(entry.getValue());
        }

        for(List<FuncInstance> gapFunctions : gapList){
            Set<Set<FuncInstance>> combinations = gapCollect(gapFunctions);
            result.add(combinations);
        }

        return convert(cartesianProduct(result,getQueryResult().noSolCounter));
    }

    public Set<List<Set<FuncInstance>>> cartesianProduct(List<Set<Set<FuncInstance>>> lists, int size) {
        Set<List<Set<FuncInstance>>> resultLists = new HashSet<>();

        // Anchor
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            Set<Set<FuncInstance>> firstList = lists.get(0);
            Set<List<Set<FuncInstance>>> remainingLists = cartesianProduct(lists.subList(1, lists.size()),size);
            for (Set<FuncInstance> condition : firstList) {
                for (List<Set<FuncInstance>> remainingList : remainingLists) {
                    ArrayList<Set<FuncInstance>> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);

                    List<FuncInstance> instances = convert(resultList);
                    int time = ExecutionTime.computeExecTime(instances);
                    double estCoverage = estimateCoverage(resultList);
                    double estReliability = estimateReliability(instances,size);

                    // TODO Clean up code
                    if(
                            (
                                    this.minPlan == null
                                            && instances.size() > 0
                                            && time <= getSetting().get("Time")
                                            && estCoverage >= getSetting().get("Coverage")
                                            && estReliability >= getSetting().get("Reliability")
                            )
                                    ||
                                    (
                                            this.minPlan != null && instances.size() > 0
                                                    && instances.size() < this.minPlan.size()
                                                    && time <= getSetting().get("Time")
                                                    && estCoverage >= getSetting().get("Coverage")
                                                    && estReliability >= getSetting().get("Reliability")
                                    )
                    ){
                        this.minPlan = instances;
                        resultLists.add(resultList);

//                        return resultLists;
                    }

                    resultLists.add(resultList);
                }
            }
        }

        return resultLists;
    }

    private double estimateCoverage(List<Set<FuncInstance>> resultList){
        double orgCoverage = ((double) getQueryResult().solCounter) / getQueryResult().getLocalSolutions().size();
        double relCoverage = Coverage2.computeCoverage(resultList, getQueryResult().noSolCounter);
        double coverageGrowth = (1-orgCoverage) * relCoverage;
        double estCoverage = orgCoverage + coverageGrowth ;

        return estCoverage;
    }

    private double estimateReliability(List<FuncInstance> instances, int size){
        double reliability = Reliability.computeReliability(1,instances);
//        reliability = (reliability + (size - 1)) / size;

        return reliability;
    }

    private Set<Set<FuncInstance>> gapCollect(List<FuncInstance> gapFunctions){
        Set<FuncInstance> gapSet = new HashSet<>(gapFunctions);

        Set<Set<FuncInstance>> set = new HashSet<>();
        for (int i = 1; i <= gapFunctions.size(); i++) {
            set.addAll(Sets.powerSet(gapSet));
        }

        return set;
    }

    private List<List<FuncInstance>> convert(Set<List<Set<FuncInstance>>> combinations){
        List<List<FuncInstance>> result = new LinkedList<>();

        for(List<Set<FuncInstance>> combination : combinations){
            List<FuncInstance> instances = new LinkedList<>();
            for(Set<FuncInstance> gapInstatiations : combination){
                instances.addAll(gapInstatiations);
            }
            result.add(instances);
        }

        return result;
    }

    private List<FuncInstance> convert(List<Set<FuncInstance>> setList){
        List<FuncInstance> list = new LinkedList<>();

        for(Set<FuncInstance> set : setList){
            list.addAll(set);
        }

        return list;
    }

    private List<GapMap> calcParetoSolutions(List<List<FuncInstance>> combinations){
        List<GapMap> gapMapList = calcSortedGapMaps(combinations);

        // Delete all plans that are not able to fulfil user preferences
        Optional<GapMap> test = gapMapList.stream().filter(gapMap -> gapMap.getPlan().getQos().getCoverage() >= this.getSetting().get("Coverage")
                && gapMap.getPlan().getQos().getReliability() >= this.getSetting().get("Reliability")
                && gapMap.getPlan().getQos().getExecTime() <= this.getSetting().get("Time")
        ).findFirst();

        GapMap optimum;
        if(!test.isPresent()) optimum = gapMapList.get(gapMapList.size()-1);
        else optimum = test.get();

        // Find the cheapest plan that is suitable according to the user preferences and delete all plans that are
        // more expensive. The list will usually contain more than one element since the solution is pareto optimal.
        gapMapList.removeIf(gapMap -> gapMap.getPlan().getQos().getCoverage() != optimum.getPlan().getQos().getCoverage()
                || gapMap.getPlan().getQos().getReliability() != optimum.getPlan().getQos().getReliability()
                || gapMap.getPlan().getQos().getExecTime() != optimum.getPlan().getQos().getExecTime()
                || gapMap.getPlan().getQos().getCalls() != optimum.getPlan().getQos().getCalls()
        );

        return gapMapList;
    }

    private List<GapMap> calcSortedGapMaps(List<List<FuncInstance>> combinations){
        List<GapMap> gapMapList = new LinkedList<>();

        // Compute for all possible combinations a service plan (and the QoS estimations)
        System.out.println("Create GapMaps");
        for(List<FuncInstance> instances : combinations){
            ServicePlan plan = new ServicePlan(instances);
            GapMap gapMap = new GapMap(plan);
            QualityOfService qos = DataQualityFactory.createFast(plan.getId(),getQueryResult(),gapMap,null);
            plan.setQos(qos);

            gapMapList.add(gapMap);
        }

        // Sort gap map according to quality (best plan first)
//        gapMapList.sort((g1,g2) -> -1 * QualityOfService.compareQuality(g1.getPlan().getQos(),g2.getPlan().getQos(),this.getSetting()));
        System.out.println("Sort Combinations");
        gapMapList.sort(Comparator.comparingInt((GapMap g) -> g.getPlan().getQos().getCalls()).thenComparing((GapMap g) -> g.getPlan().getQos().getExecTime()));

        return gapMapList;
    }

    private GapMap selectParetoSolution(List<GapMap> paretoSolutions){
        paretoSolutions.sort(Comparator.comparingInt(g -> g.getPlan().getQos().getCalls()));
        paretoSolutions.removeIf(g -> g.getPlan().getQos().getCalls() > paretoSolutions.get(0).getPlan().getQos().getCalls());
        paretoSolutions.sort(Comparator.comparingInt(g -> g.getPlan().getQos().getExecTime()));

//        Collections.reverse(paretoSolutions);

        return paretoSolutions.get(0);
    }

    private void writePlansToDisk(List<GapMap> paretoSolutions){
        FileWriter myWriter = null;
        try {
            // TODO Make path adjustable
            myWriter = new FileWriter("res/pareto/pareto.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JSONObject obj = new JSONObject();
        JSONArray plans = new JSONArray();

        for(GapMap solution : paretoSolutions){
            JSONObject sol = new JSONObject();

            JSONObject qosJson = new JSONObject();
            qosJson.put("coverage",solution.getPlan().getQos().getCoverage());
            qosJson.put("reliability",solution.getPlan().getQos().getReliability());
            qosJson.put("time",solution.getPlan().getQos().getExecTime());
            sol.put("quality",qosJson);

            JSONArray instantiations = new JSONArray();
            for(FuncInstance instance : solution.getPlan().toList()){
                instantiations.put(instance.toString());
            }
            sol.put("instantiations",instantiations);

            plans.put(sol);
        }
        obj.put("plans",plans);

        try {
            myWriter.write(obj.toString());
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
