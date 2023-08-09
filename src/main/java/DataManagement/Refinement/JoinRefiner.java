package DataManagement.Refinement;

import QueryManagement.Query.TunableQuery;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;

import java.util.*;

public class JoinRefiner {
    static List<List<String>> handleJoins(TunableQuery tq, List<List<String>> result){
        List<String> resultVars = tq.getOriginalResultVars();
        List<String> subjectVars = tq.getSubjectVars();
        resultVars.removeAll(subjectVars);

        Map<String, Pair<String,String>> addedVarsMap = tq.getAddedVarsMap();
        resultVars.removeIf(addedVarsMap::containsKey);

        List<List<String>> refinedResult = new LinkedList<>();
        refinedResult.add(resultVars);

        Set<Triple> optionalTriples = tq.getOptionalTriples();
        Set<Integer> optIndexSet = new HashSet<>();
        Set<Integer> subjectIndexes = new HashSet<>();

        for(Triple triple : optionalTriples){
            for (int i = 0; i < result.get(0).size(); i++) {
                if(result.get(0).get(i).equals(triple.getObject().getName())) optIndexSet.add(i);
                if(subjectVars.contains(result.get(0).get(i))) subjectIndexes.add(i);
            }
        }

        for (int i = 0; i < result.get(0).size(); i++) {
            if(subjectVars.contains(result.get(0).get(i))) subjectIndexes.add(i);
        }

        for (int i = 1; i < result.size(); i++) {
            if(isValidRow(result.get(i),optIndexSet)) {
                List<String> row = new LinkedList<>();

                for (int j = 0; j < result.get(i).size(); j++) {
                    if(!subjectIndexes.contains(j)) row.add(result.get(i).get(j));
                }

                refinedResult.add(row);
            }
        }

        return refinedResult;
    }

    private static boolean isValidRow(List<String> row, Set<Integer> optIndexSet){
        for (int j = 0; j < row.size(); j++) {
            if(row.get(j) == null && !optIndexSet.contains(j)) return false;
        }

        return true;
    }
}
