package ServiceManagement.Plan;

import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.DataQualityFactory;
import ServiceManagement.DataQuality.QualityOfService;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;

public class ServicePlan implements Cloneable {
    private static int counter = 1001;
    private int id;
    private List<FuncInstance> instances;
    private QualityOfService qos;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public ServicePlan() {
        this.instances = new LinkedList<>();
    }

    public ServicePlan(List<FuncInstance> instances) {
        this.id = counter++;
        this.instances = instances;
        this.qos = DataQualityFactory.create(id, instances); // TODO EXPERIMENTAL
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public GapMap removeRedundantCalls(){
        GapMap gapMap = new GapMap(this);
        this.instances = removeAndMerge();
        gapMap.computeHelper();
        this.qos = DataQualityFactory.create(this.id,instances);

        return gapMap;
    }

    public int getId() {
        return id;
    }

    public QualityOfService getQos(){
        return this.qos;
    }

    public boolean add(FuncInstance instance){
        return this.instances.add(instance);
    }

    public List<FuncInstance> toList() {
        return instances;
    }

    public int callSize(){
        return this.instances.size();
    }

    public void setQos(QualityOfService qos){
        this.qos = qos;
    }

    // TODO IMPROVE
    public void removeInstances(QueryResult qr, GapMap gapMap, List<FuncInstance> removableInstances){
        this.instances.removeAll(removableInstances);
//        this.qos = DataQualityFactory.create(this.id,instances,removableInstances); // TODO IMPORTANT
        this.qos = DataQualityFactory.createFast(this.id,qr,gapMap,removableInstances);
    }

    @Override
    public String toString() {
        String meta = "PlanID " + this.id;

        return "{ " + meta + ", QoS: " + this.qos + " }";
    }

    @Override
    public ServicePlan clone(){
        try {
            ServicePlan clone = (ServicePlan) super.clone();

            List<FuncInstance> clonedPlan = new LinkedList<>();
            for(FuncInstance instance : this.instances){
                clonedPlan.add(instance);
            }
            clone.instances = clonedPlan;

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServicePlan that = (ServicePlan) o;
        return Objects.equals(instances, that.instances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.instances);
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private List<FuncInstance> removeAndMerge(){
        List<FuncInstance> cleanedInstanceList = new LinkedList<>();

        for(FuncInstance instance : this.instances){
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

    // *****************************************************************************************************************
    // Static Methods
    // *****************************************************************************************************************

    public static Map<Pair<Integer,String>, List<FuncInstance>> computeGapMap(List<FuncInstance> instances){
        Map<Pair<Integer,String>,List<FuncInstance>> gapSets = new HashMap<>();

        for(FuncInstance instance : instances){
            for(Alignment alignment : instance.getSelectedAlignments()){
                StringBuilder key = new StringBuilder();
                for (int i = 0; i < alignment.getLocalRelations().size(); i++) {
                    LocalPath path = alignment.getLocalRelations().get(i);
                    if(i+1 < alignment.getLocalRelations().size()) key.append(path.get(path.size() - 1)).append(",");
                    else key.append(path.get(path.size()-1));
                }

                Pair<Integer,String> keyPair = new Pair<>(instance.getRowId(),key.toString());
                List<FuncInstance> list = new LinkedList<>();
                if(gapSets.containsKey(keyPair)) list = gapSets.get(keyPair);
                if(!list.contains(instance)) list.add(instance);
                gapSets.put(keyPair,list);
            }
        }

        return gapSets;
    }




}
