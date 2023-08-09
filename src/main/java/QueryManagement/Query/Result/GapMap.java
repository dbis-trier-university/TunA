package QueryManagement.Query.Result;

import FunctionStore.Function.FuncInstance;
import FunctionStore.FunctionStore;
import ServiceManagement.DataQuality.Metrics.Coverage;
import ServiceManagement.DataQuality.Metrics.Reliability;
import ServiceManagement.Plan.OptServicePlan;
import ServiceManagement.Plan.ServicePlan;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;

public class GapMap {
    private final Map<Pair<Integer,String>, List<FuncInstance>> gapMap;
    private Map<FuncInstance, Pair<Integer,String>> inverseGapMap;
    private Map<String,Integer> execTimeMap = null;
    private ServicePlan plan;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public GapMap(ServicePlan servicePlan){
        this.plan = servicePlan;
        this.gapMap = ServicePlan.computeGapMap(this.plan.toList());
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************


    public Map<Pair<Integer, String>, List<FuncInstance>> getGapMap() {
        return gapMap;
    }

    public int size(){
        return this.gapMap.size();
    }

    public void setPlan(ServicePlan plan){
        this.plan = plan;
        computeTimeMap();
        invert();
    }

    public Set<Map.Entry<Pair<Integer, String>, List<FuncInstance>>> entrySet(){
        return this.gapMap.entrySet();
    }

    public ServicePlan getPlan() {
        return this.plan;
    }

    public OptServicePlan getOptPlan() {
        if(this.plan instanceof  OptServicePlan optServicePlan) return optServicePlan;
        else return null;
    }


    public Pair<Integer,String> getGap(FuncInstance instance){
        return this.inverseGapMap.get(instance);
    }

    public List<FuncInstance> getInstances(String apiName){
        List<FuncInstance> instances = new LinkedList<>();

        for(FuncInstance instance : plan.toList()){
            if(instance.getApiName().equalsIgnoreCase(apiName)) instances.add(instance);
        }

        return instances;
    }

    public Map<String,Integer> getTimeMap(){
        return this.execTimeMap;
    }

    public String getMostExpensiveApiName(){
        return getTimeMap().entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    public Map<FuncInstance, List<Pair<Integer,String>>> getUsage(String apiName) {
        Map<FuncInstance,List<Pair<Integer,String>>> usedMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
            for(FuncInstance instance : entry.getValue()){
                if(instance.getApiName().equals(apiName)){
                    List<Pair<Integer,String>> list = new LinkedList<>();
                    if(usedMap.containsKey(instance)){
                        list = usedMap.get(instance);
                    }
                    list.add(entry.getKey());
                    usedMap.put(instance,list);
                }
            }
        }

        return usedMap;
    }

    public Map<Pair<Integer,String>,Integer> getBackup(String apiName) {
        Map<Pair<Integer,String>,Integer> backupMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
            for(FuncInstance instance : entry.getValue()){
                if(instance.getApiName().equals(apiName)){
                    backupMap.put(entry.getKey(),entry.getValue().size());
                }
            }
        }

        return backupMap;
    }

    public Map<Pair<Integer,String>,Double> getCoverage(){
        Map<Pair<Integer,String>,Double> coverageMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
            coverageMap.put(entry.getKey(), Coverage.computeCoverage(-1,entry.getValue()));
        }

        return coverageMap;
    }

    public Map<Pair<Integer,String>,Double> getReliability(){
        Map<Pair<Integer,String>,Double> reliabilityMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
            reliabilityMap.put(entry.getKey(), Reliability.computeReliability(-1, entry.getValue()));
        }

        return reliabilityMap;
    }

    public boolean hasBackUp(FuncInstance instance){
        Pair<Integer,String> pair = this.inverseGapMap.get(instance);
        return this.gapMap.get(pair).size() > 2;
    }

    public void remove(String apiName, QueryResult qr, List<FuncInstance> removableInstances){
            this.plan.removeInstances(qr,this,removableInstances); // TODO IMPORTANT

            // TODO IMPORTANT
            this.gapMap.forEach((k,v) -> {
                v.removeAll(removableInstances);
            });

            int time = 0;
            for(FuncInstance instance : removableInstances){
                int responseTime = FunctionStore.get(instance.getApiName()).getResponseTime();
                int timeout = instance.getService().getTimeout();

                time += responseTime + timeout;
            }

            this.execTimeMap.put(apiName,this.execTimeMap.get(apiName) - time);
    }

    public void computeOptPlan(){
        this.plan = new OptServicePlan(plan.getId(),plan.toList(),plan.getQos());
    }

    public void computeHelper(){
        computeTimeMap();
        invert();
    }

    public String toString(){
        return this.plan.getQos().toString();
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private void computeTimeMap(){
        Map<String,Integer> execTimeMap = new HashMap<>();

        for (FuncInstance instance : this.plan.toList()) {
            int responseTime = FunctionStore.get(instance.getApiName()).getResponseTime();
            int timeout = instance.getService().getTimeout();

            if (execTimeMap.containsKey(instance.getApiName())) {
                int time = execTimeMap.get(instance.getApiName());
                execTimeMap.put(instance.getApiName(), time + responseTime + timeout);
            } else {
                execTimeMap.put(instance.getApiName(), responseTime);
            }
        }

        this.execTimeMap = execTimeMap;
    }

    private void invert(){
        this.inverseGapMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>, List<FuncInstance>> entry : this.gapMap.entrySet()){
            for(FuncInstance instance : entry.getValue()){
                this.inverseGapMap.put(instance,entry.getKey());
            }
        }
    }
}
