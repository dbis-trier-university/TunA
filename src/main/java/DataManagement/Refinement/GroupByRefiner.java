package DataManagement.Refinement;

import QueryManagement.Query.TunableQuery;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;

import java.util.*;

public class GroupByRefiner {
    static List<List<String>> handleGroupBy(List<List<String>> result, TunableQuery tq){
        Map<Var, Expr> selectExpr = tq.getSelectExpressions();

        List<List<String>> refinedResult = new LinkedList<>();
        refinedResult.add(tq.getOriginalResultVars()); // add original header

        // First sort for faster processing
        List<String> header = result.get(0);
        result = sort(result);

        // TODO This can currently only handle simple aggregates
        for(Map.Entry<Var,Expr> aggEntry : selectExpr.entrySet()){
            int aggPos = getAggregateCol(aggEntry,header);

            List<List<String>> tmpResult = new LinkedList<>();
            for(List<String> row : result){
                List<String> rowList = new LinkedList<>();

                for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                    String colValue = row.get(colIndex);

                    if(colIndex == aggPos) {
                        rowList.add("1");
                    } else {
                        rowList.add(colValue);
                    }
                }

                tmpResult.add(rowList);
            }

            Set<String> handled = new HashSet<>();
            for (int i = 0; i < tmpResult.size(); i++) {
                int counter = 1;

                String key = "";
                for(String str : tmpResult.get(i)){
                    key += str;
                }

                if(!handled.contains(key)){
                    for (int j = i+1; j < tmpResult.size(); j++) {
                        if(matching(tmpResult.get(i),tmpResult.get(j))) counter++;
                    }

                    List<String> row = new LinkedList<>();
                    for (int j = 0; j < tmpResult.get(i).size(); j++) {
                        if(j == aggPos) row.add(counter+"");
                        else row.add(tmpResult.get(i).get(j));
                    }

                    refinedResult.add(row);
                    handled.add(key);
                }
            }
        }

        return refinedResult;
    }

    private static List<List<String>> sort(List<List<String>> result){
        result = result.subList(1, result.size());

        result.sort((l1,l2) -> {
            for (int i = 0; i < l1.size(); i++) {
                String l1i = l1.get(i);
                if(l1i == null) l1i = "";
                String l2i = l2.get(i);
                if(l2i == null) l2i = "";

                // direction -1 = ASC
                // direction -2 = DESC
                int comp = l1i.compareTo(l2i);
                if(comp != 0) return comp;
            }

            return 0;
        });

        return result;
    }

    private static int getAggregateCol(Map.Entry<Var,Expr> aggEntry, List<String> header){
        String aggVar = ((ExprAggregator) aggEntry.getValue()).getAggregator().getExprList().get(0).getVarName();

        int aggPos = 0;
        for (; aggPos < header.size(); aggPos++) {
            if(header.get(aggPos).equals(aggVar)) break;
        }

        return aggPos;
    }


    private static boolean matching(List<String> row1, List<String> row2){
        if(row1.size() != row2.size()) return false;

        for (int i = 0; i < row1.size(); i++) {
            if(row1.get(i) == null && row2.get(i) != null || row1.get(i) != null && row2.get(i) == null) return false;
            if(row1.get(i) != null && row2.get(i) != null && !row1.get(i).equals(row2.get(i))) return false;
        }

        return true;
    }
}
