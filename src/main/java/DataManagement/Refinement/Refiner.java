package DataManagement.Refinement;

import QueryManagement.Query.TunableQuery;

import java.util.LinkedList;
import java.util.List;

public class Refiner {

    public static List<List<String>> refinement(TunableQuery tq, List<List<String>> result){
        List<List<String>> refinedResult = result;
        refinedResult = JoinRefiner.handleJoins(tq,refinedResult);

        if(tq.isDistinct()) refinedResult = handleDistinct(refinedResult);
        if(tq.hasFilter()) refinedResult = FilterRefiner.handleFilter(refinedResult,tq.getFilterExpr());
        if(tq.hasGroupBy()) refinedResult = GroupByRefiner.handleGroupBy(refinedResult,tq);
        if(tq.hasOrderBy()) refinedResult = OrderByRefiner.handleOrderBy(refinedResult, tq.getOrderByConditions());
        if(tq.hasLimit()) refinedResult = refinedResult.subList(0,tq.getLimit()+1);
        // TODO implementation of minus and not exists filter

        return refinedResult;
    }

    private static List<List<String>> handleDistinct(List<List<String>> result){
        List<List<String>> refinedResult = new LinkedList<>();
        refinedResult.add(result.get(0)); // Add header

        for (int i = 1; i < result.size(); i++) {
            List<String> row = result.get(i);
            if(!contains(refinedResult,row)) refinedResult.add(row);
        }

        return refinedResult;
    }

    private static boolean contains(List<List<String>> refinedResult, List<String> row){
        String rowStr = "";
        for(String str : row) rowStr += str + " ";

        for (int i = 1; i < refinedResult.size(); i++) {
            List<String> refinedRow = refinedResult.get(i);

            String refRowStr = "";
            for(String str : refinedRow) refRowStr += str + " ";

            if(rowStr.equals(refRowStr)) return true;
        }

        return false;
    }
}
