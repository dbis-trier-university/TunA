package DataManagement.Refinement;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.SortCondition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OrderByRefiner {
    static List<List<String>> handleOrderBy(List<List<String>> result, List<SortCondition> sortConditions){
        List<List<String>> sortedResult = new LinkedList<>();
        sortedResult.add(result.get(0));
        result = result.subList(1,result.size());

        Map<Integer,Integer> map = new HashMap<>();
        for (int i = 0; i < sortedResult.get(0).size(); i++) {
            for(SortCondition condition : sortConditions){
                if(sortedResult.get(0).get(i).equals(condition.getExpression().getVarName())) map.put(i,condition.getDirection());
            }
        }

        result.sort((l1,l2) -> {
            for(Map.Entry<Integer,Integer> entry : map.entrySet()){
                String l1i = l1.get(entry.getKey());
                if(l1i == null) l1i = "";
                String l2i = l2.get(entry.getKey());
                if(l2i == null) l2i = "";

                // direction -1 = ASC
                // direction -2 = DESC
                int factor = -1;
                if(entry.getValue() == -2) factor = 1;
                int comp;

                if(StringUtils.isNumeric(l1i) && StringUtils.isNumeric(l2i)){
                    Integer l1int = Integer.parseInt(l1i);
                    Integer l2int = Integer.parseInt(l2i);

                    comp = l1int.compareTo(l2int);
                } else {
                    comp = l1i.compareTo(l2i);
                }

                if(comp != 0) return factor * comp;
            }

            return 0;
        });
        sortedResult.addAll(result);

        return sortedResult;
    }
}
