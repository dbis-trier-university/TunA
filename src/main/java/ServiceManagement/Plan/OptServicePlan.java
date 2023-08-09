package ServiceManagement.Plan;

import Configuration.Configuration;
import FunctionStore.Function.FuncInstance;
import QueryManagement.Query.Result.GapMap;
import ServiceManagement.DataQuality.DataQualityFactory;
import ServiceManagement.DataQuality.QualityOfService;

import java.util.*;
import java.util.logging.Logger;

public class OptServicePlan extends ServicePlan implements Cloneable {
    private static final Logger logger = Configuration.getInstance().getLogger();
    private static int counter = 1001;
    private int id;
    private List<Set<FuncInstance>> optPlan;
    private QualityOfService qos;


    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    // TODO remove
    private OptServicePlan(List<Set<FuncInstance>> optPlan, QualityOfService qos) {
        super(convertToList(optPlan));
        this.id = counter++;
        this.optPlan = optPlan;
        this.qos = qos;
    }

    public OptServicePlan(int id, List<FuncInstance> instances, QualityOfService qos){
        super(instances);
        this.id = id;
        this.optPlan = group(instances);
        this.qos = qos;
    }


    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public void remove(FuncInstance instance){
        this.toList().remove(instance);
    }

    public int getId() {
        return id;
    }

    public QualityOfService getQos() {
        return qos;
    }

    @Override
    public List<FuncInstance> toList() {
        List<FuncInstance> list = new LinkedList<>();

        for(Set<FuncInstance> set : this.optPlan){
            list.addAll(set);
        }

        return list;
    }

    public List<Set<FuncInstance>> getAsList() {
        return optPlan;
    }

    @Override
    public String toString(){
        String meta = "PlanID " + this.id;
        String calls = "Groups: " + this.optPlan.size();

        return "{ " + meta + ", " + calls + ", QoS: " + this.qos + " }";
    }

    public static GapMap removeRedundantCalls(ServicePlan plan){
        GapMap gapMap = new GapMap(plan);

        logger.finer("Start optimizing a single service plan: " + plan);
        List<Set<FuncInstance>> optPlanList = remove(gapMap);

        logger.finer("Compute quality of service information");
        OptServicePlan optPlan = new OptServicePlan(optPlanList,null);
        optPlan.qos = DataQualityFactory.create(optPlan.id, optPlan.toList());
        gapMap.setPlan(optPlan);

        return gapMap;
    }

    @Override
    public OptServicePlan clone(){
        OptServicePlan clone = (OptServicePlan) super.clone();

        List<Set<FuncInstance>> clonedPlan = new LinkedList<>();
        for(Set<FuncInstance> set : this.optPlan){
            clonedPlan.add(new HashSet<>(set));
        }
        clone.optPlan = clonedPlan;

        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptServicePlan that = (OptServicePlan) o;
        return Objects.equals(optPlan, that.optPlan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optPlan);
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private static List<Set<FuncInstance>> remove(GapMap gapMap){
        logger.finest("Remove and merge unnecessary calls");
        List<FuncInstance> cleanedInstanceList = removeAndMerge(gapMap);

        return group(cleanedInstanceList);
    }

    private static List<Set<FuncInstance>> group(List<FuncInstance> cleanedInstanceList){
        logger.finest("Presetting the groupings map with zeros");
        Map<FuncInstance,Integer> groupMap = new HashMap<>();
        for(FuncInstance instance : cleanedInstanceList){
            groupMap.put(instance,0);
        }

        logger.finest("Associate each API to an id that stands for a group of APIs that can be called parallel.");
        int noOfGroups = 0;
        for (;!isGrouped(groupMap); noOfGroups++) {
            for(FuncInstance instance : cleanedInstanceList){
                if(groupMap.get(instance) == 0 && !contains(groupMap,instance,noOfGroups)){
                    groupMap.put(instance,noOfGroups);
                }
            }
        }

        logger.finest("Convert the map to a list of sets");
        List<Set<FuncInstance>> optPlan = new LinkedList<>();
        for (int i = 1; i < noOfGroups; i++) {
            Set<FuncInstance> group = new HashSet<>();

            for(Map.Entry<FuncInstance,Integer> entry : groupMap.entrySet()){
                if(entry.getValue() == i) group.add(entry.getKey());
            }

            optPlan.add(group);
        }

        return optPlan;
    }

    private static List<FuncInstance> removeAndMerge(GapMap gapMap){
        List<FuncInstance> cleanedInstanceList = new LinkedList<>();

        for(FuncInstance instance : gapMap.getPlan().toList()){
            FuncInstance updatableInstance = null;
            for(FuncInstance cleanInstance : cleanedInstanceList){
                if(cleanInstance.getUrl().equals(instance.getUrl())) {
                    updatableInstance = cleanInstance;
                }
            }

            if(updatableInstance == null) {
                cleanedInstanceList.add(instance);
            } else {
                Iterator<FuncInstance> it = cleanedInstanceList.iterator();
                while (it.hasNext()){
                    FuncInstance cleanInstance = it.next();
                    if(cleanInstance.getUrl().equals(instance.getUrl())){
                        cleanInstance.addSelectedAlignments(instance.getSelectedAlignments());
                    }
                }
            }
        }

        return cleanedInstanceList;
    }

    private static boolean isGrouped(Map<FuncInstance,Integer> groupMap){
        for(Map.Entry<FuncInstance,Integer> entry : groupMap.entrySet()){
            if(entry.getValue() == 0) return false;
        }

        return true;
    }

    private static boolean contains(Map<FuncInstance,Integer> groupMap, FuncInstance instance, int index){
        for(Map.Entry<FuncInstance,Integer> entry : groupMap.entrySet()){
            if(entry.getValue() == index && entry.getKey().getApiName().equals(instance.getApiName())) return true;
        }

        return false;
    }

    private static List<FuncInstance> convertToList(List<Set<FuncInstance>> optPlan){
        List<FuncInstance> list = new LinkedList<>();

        for(Set<FuncInstance> set : optPlan){
            list.addAll(set);
        }

        return list;
    }
}
