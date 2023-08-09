package FunctionStore.Function;

import Configuration.Configuration;
import Configuration.Service.ServiceObject;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;
import QueryManagement.Query.Result.MissingResult;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FuncInstance implements Comparable {
    private String apiName, dbName, url, pcValue;
    private int rowId;
    private double responseProbability;
    private Set<Alignment> selectedAlignments;
    private ServiceObject service;
    private List<Alignment> alignments;

    /*******************************************************************************************************************
     * Constructor
     ******************************************************************************************************************/

    public FuncInstance(int rowId, String label, String dBname, String pcValue, double responseProbability,
                        Set<Alignment> selectedAlignments, List<Alignment> alignments)
    {
        this.rowId = rowId;
        this.apiName = label;
        this.dbName = dBname;
        this.pcValue = pcValue;
        this.responseProbability = responseProbability;
        this.selectedAlignments = selectedAlignments;
        this.alignments = alignments;

        this.service = Configuration.getInstance().getServiceObject(apiName);
        this.url = this.service.buildSingleParameterCallUrl(pcValue);
    }

    /*******************************************************************************************************************
     * Public Methods
     ******************************************************************************************************************/

    public static List<FuncInstance> createFunctionInstances(MissingResult entry, List<Function> functionStore){
        List<FuncInstance> instances = new LinkedList<>();

        // Check for each function if it is a suitable function to call
        for(Function function : functionStore){
            for(Alignment alignment : function.getAlignments()){
                // Check each output path of a function
                for(LocalPath localPath : alignment.getLocalRelations()){
                    String pcValue = entry.getRelValueMap().get(function.getInputRelation());

                    // check if the last relation in the local path is equal to the output relation of the function
                    // Afterwards instantiate each suitable API and add it to the list of possible API calls
                    if(localPath.isLast(entry.getRelationName()) && pcValue != null) {
                        Set<Alignment> sa = new HashSet<>();
                        sa.add(function.getAlignment(entry.getRelationName()));

                        FuncInstance instance = function.create(entry.getRowId(),pcValue,sa,function.getAlignments());
                        instances.add(instance);
                    }
                }
            }
        }

        return instances;
    }

    public int getRowId() {
        return rowId;
    }

    public String getApiName() {
        return apiName;
    }

    public String getDbName() {
        return dbName;
    }

    public String getPreConditionValue() {
        return pcValue;
    }

    public String getUrl() {
        return url;
    }

    public double getResponseProbability() {
        return responseProbability;
    }

    public Set<Alignment> getSelectedAlignments() {
        return selectedAlignments;
    }

    public Alignment getSelectedAlignment(String relation){
        for(Alignment alignment : alignments){
            for(LocalPath path : alignment.getLocalRelations()){
                if(path.get(path.size()-1).equals(relation)) return alignment;
            }
        }

        return null;
    }

    public ServiceObject getService() {
        return service;
    }

    public List<Alignment> getAlignments() {
        return alignments;
    }

    public void addSelectedAlignments(Set<Alignment> alignment){
        this.selectedAlignments.addAll(alignment);
    }

//    @Override
//    public boolean equals(Object object){
//        if(object instanceof FuncInstance instance){
//            return this.url.equalsIgnoreCase(instance.getUrl())
//                    && this.rowId == instance.getRowId()
//                    && this.selectedAlignments.equals(instance.getSelectedAlignments());
//        }
//
//        return false;
//    }
//
//    @Override
//    public int hashCode() {
//        String string = this.url + this.rowId + this.selectedAlignments;
//        return string.hashCode();
//    }

    @Override
    public String toString() {
        return this.url;
    }

    @Override
    public int compareTo(Object o) {
        FuncInstance instance = (FuncInstance) o;
        return this.getUrl().compareTo(instance.getUrl());
    }
}
