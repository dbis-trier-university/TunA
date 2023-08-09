package QueryManagement.Query.Result;


import java.util.List;
import java.util.Map;

public class MissingResult {
    private int rowId;
    private String varName, relationName;
    private Map<String,String> relValueMap;

    public MissingResult(int rowId, String varName, String relationName, Map<String,String> relValueList) {
        this.rowId = rowId;
        this.varName = varName;
        this.relationName = relationName;
        this.relValueMap = relValueList;
    }

    public int getRowId() {
        return rowId;
    }

    public String getVarName() {
        return varName;
    }

    public String getRelationName() {
        return relationName;
    }

    public Map<String,String> getRelValueMap() {
        return relValueMap;
    }

    @Override
    public String toString(){
        StringBuilder string = new StringBuilder("( " + this.rowId + " ; " + this.varName+" ; { ");

        List<Map.Entry<String,String>> relValueList = relValueMap.entrySet().stream().toList();
        for (int i = 0; i < relValueMap.size(); i++) {
            Map.Entry<String,String> entry = relValueList.get(i);

            if(i + 1 == relValueMap.size()-1) string.append(entry.getValue()).append(" ; ");
            else string.append(entry.getValue());
        }

        return string + " } )";
    }
}
