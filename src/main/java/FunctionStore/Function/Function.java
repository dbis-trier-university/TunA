package FunctionStore.Function;

import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;

import java.util.List;
import java.util.Set;

public class Function {
    private static int counter = 1000;
    private final int id;
    private String dbName, apiName, inputType, inputRelation;
    private List<Alignment> alignments;
    private int responseTime;
    private double responseProbability;

    /*******************************************************************************************************************
     * Constructor
     ******************************************************************************************************************/

    public Function(String dbName, String apiName, String inputType, String inputRelation, List<Alignment> alignment,
                    int responseTime, double responseProbability)
    {
        this.id = counter++;
        this.dbName = dbName;
        this.apiName = apiName;
        this.inputType = inputType;
        this.inputRelation = inputRelation;
        this.alignments = alignment;
        this.responseTime = responseTime;
        this.responseProbability = responseProbability;
    }

    /*******************************************************************************************************************
     * Public Methods
     ******************************************************************************************************************/

    public FuncInstance create(int rowId, String pcValue, Set<Alignment> selection, List<Alignment> alignments){
        return new FuncInstance(rowId, this.apiName, this.dbName,pcValue, this.responseProbability,
                selection,alignments);
    }

    public int getId() {
        return id;
    }

    public String getDbName() {
        return dbName;
    }

    public String getApiName() {
        return apiName;
    }

    public String getInputType() {
        return inputType;
    }

    public String getInputRelation() {
        return inputRelation;
    }

    public List<Alignment> getAlignments() {
        return alignments;
    }

    public Alignment getAlignment(String relation){
        for(Alignment alignment : this.alignments){
            for(LocalPath localPath : alignment.getLocalRelations()){
                if(localPath.isLast(relation)) return alignment;
            }
        }

        return null;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public double getResponseProbability() {
        return responseProbability;
    }

}
