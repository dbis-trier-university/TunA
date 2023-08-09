package Configuration.Service;

public class Parameter {
    private String name, inputType, inputRelation;

    public Parameter(String name, String inputType, String inputRelation) {
        this.name = name;
        this.inputType = inputType;
        this.inputRelation = inputRelation;
    }

    /*******************************************************************************************************************
     * Getter and Setter
     ******************************************************************************************************************/

    public String getName() {
        return name;
    }

    public String getInputType() {
        return inputType;
    }

    public String getInputRelation() {
        return inputRelation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public void setInputRelation(String inputRelation) {
        this.inputRelation = inputRelation;
    }
}
