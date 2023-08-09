package QueryManagement.Query.Result;

import QueryManagement.Query.TunableQuery;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.query.QuerySolution;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QueryResult {
    TunableQuery tunableQuery;
    List<QuerySolution> localSolutions;
    Map<String, Pair<String,String>> resultVarsMap, addedVarsMap;
    List<MissingResult> missingResultEntryMap;
    public int solCounter = 0;
    public int noSolCounter = 0;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public QueryResult(TunableQuery tunableQuery, List<QuerySolution> localSolutions) {
        this.tunableQuery = tunableQuery;
        this.localSolutions = localSolutions;
        this.addedVarsMap = this.tunableQuery.getAddedVarsMap();
        this.resultVarsMap = this.tunableQuery.getResultVarsMap();
        this.missingResultEntryMap = new LinkedList<>();

        identifyMissingResults();
        collectAvailableData();
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    // Refactor return type
    public List<MissingResult> getMissingResultEntryMap() {
        return this.missingResultEntryMap;
    }

    public List<QuerySolution> getLocalSolutions() {
        return localSolutions;
    }

    public Map<String, Pair<String, String>> getResultVarsMap() {
        return resultVarsMap;
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private void collectAvailableData(){
        List<String> orgVars = this.tunableQuery.getOriginalResultVars();

        for (int row = 0; row < localSolutions.size(); row++) {
            QuerySolution solution = localSolutions.get(row);

            for (int col = 0; col < orgVars.size(); col++) {
                String var = orgVars.get(col);

                if(solution.get(var) != null) solCounter++;
                else noSolCounter++;
            }
        }
    }

    private void identifyMissingResults(){
        List<String> resultVars = tunableQuery.getResultVars();
        resultVars.removeIf(addedVarsMap::containsKey);

        // Process each row in table result
        for (int row = 0; row < localSolutions.size(); row++) {
            processColumn(row,resultVars);
        }
    }

    private void processColumn(int row, List<String> resultVars){
        QuerySolution solution = this.localSolutions.get(row);

        for(String var : resultVars){
            // Checking if data in result table is missing
            if(solution.get(var) == null){
                processAddedColumns(solution,row,var);
            }
        }
    }

    private void processAddedColumns(QuerySolution solution, int row, String var){
        // This map is used to store all pre-condition relations and its values
        Map<String,String> pcValueMap = new HashMap<>();

        // Search in the added columns for pre-conditions and them to the map
        for(Map.Entry<String,Pair<String,String>> pcEntry : addedVarsMap.entrySet()){
            if(solution.get(pcEntry.getKey()) != null){
                // Adding pre-condition relation and value to map
                String pcRelation = pcEntry.getValue().getLeft();
                String pcValue = solution.get(pcEntry.getKey()).toString();
                pcValueMap.put(pcRelation,pcValue);
            }
        }

        missingResultEntryMap.add(new MissingResult(row,var,resultVarsMap.get(var).getLeft(), pcValueMap));
    }
}
