package DataManagement.Refinement;

import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

public class FilterRefiner {
    public static List<List<String>> handleFilter(List<List<String>> result, ExprList filterExpr){
        List<List<String>> refinedResult = new LinkedList<>();
        refinedResult.add(result.get(0)); // add header

        for (int i = 1; i < result.size(); i++) {
            List<String> row = result.get(i);

            for (int j = 0; j < filterExpr.size(); j++) {
                Expr expr = filterExpr.get(j);

                if(expr instanceof E_GreaterThan greaterThan){
                    String varName = greaterThan.getArg1().asVar().getVarName();
                    BigInteger value = ((NodeValueInteger) greaterThan.getArg2()).getInteger();

                    int colNumber = getColNumber(result,varName);
                    if(row.get(colNumber) != null && BigInteger.valueOf(Integer.parseInt(row.get(colNumber))).compareTo(value) > 0)
                    {
                        refinedResult.add(row);
                    }

                } else if(expr instanceof E_GreaterThanOrEqual greaterThanOrEqual){
                    String varName = greaterThanOrEqual.getArg1().asVar().getVarName();
                    BigInteger value = ((NodeValueInteger) greaterThanOrEqual.getArg2()).getInteger();

                    int colNumber = getColNumber(result,varName);
                    if(row.get(colNumber) != null && BigInteger.valueOf(Integer.parseInt(row.get(colNumber))).compareTo(value) >= 0)
                    {
                        refinedResult.add(row);
                    }
                } else if (expr instanceof E_Regex regex){
                    List<Expr> list = regex.getArgs();
                    String varName = list.get(0).getExprVar().getAsNode().getName();
                    String regexStr = list.get(1).getConstant().getString();

                    int colNumber = getColNumber(result,varName);
                    if(row.get(colNumber) != null && row.get(colNumber).matches(".*" + regexStr + ".*")) {
                        refinedResult.add(row);
                    }
                } // TODO NOT EXISTS
            }
        }

        return refinedResult;
    }

    private static int getColNumber(List<List<String>> result, String varName){
        int colNumber = 0;
        for (; colNumber < result.get(0).size(); colNumber++) {
            if(result.get(0).get(colNumber).equals(varName)) break;
        }

        return colNumber;
    }
}
