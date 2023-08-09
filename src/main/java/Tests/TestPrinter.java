package Tests;

import QueryManagement.Processor.QueryProcessor;
import QueryManagement.Processor.TunableQueryProcessor;
import Utils.Utils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.algebra.OpAsQuery;

import java.util.List;

public class TestPrinter {
    public static void printPreconditionQueries(TunableQueryProcessor tqp, String transQuery, String name){
        System.out.println("-------------------------------------------------------------------");
        System.out.println("Query: " + name + "");
        System.out.println("-------------------------------------------------------------------\n");

        System.out.println("Original Query:");
        System.out.println(tqp.getTQuery().getOriginalQuery());

        System.out.println("Extended Query:");
        Query transformedQuery = OpAsQuery.asQuery(tqp.getTQuery().getPreconditionAlgebra());
        System.out.println(transformedQuery);

        System.out.println("Manually Transformed Query:");
        System.out.println(transQuery);
    }

    public static void printTransformedQueries(TunableQueryProcessor tqp, String transQuery, String name) {
        System.out.println("-------------------------------------------------------------------");
        System.out.println("Query: " + name + "");
        System.out.println("-------------------------------------------------------------------\n");

        System.out.println("Original Query:");
        Query originalQuery = tqp.getTQuery().getOriginalQuery();
        System.out.println(originalQuery);

        System.out.println("Extended Query:");
        Query extendedQuery = OpAsQuery.asQuery(tqp.getTQuery().getCurrentAlgebra());
        System.out.println(extendedQuery);

        System.out.println("Manually Transformed Query:");
        System.out.println(transQuery);
    }

    public static void printStatus(List<String> failedQueries, String dir, int failed){
        if(failed == 0) System.out.println("All queries successful");
        else {
            int size = Utils.getFileNames(dir).size();
            System.out.println(size-failed + "/" + size + " queries successful");

            System.out.println("Failed Queries:");
            for(String fileName : failedQueries){
                System.out.println("\t" + fileName + " failed..");
            }
        }
    }

    public static void printQueryResult(QueryProcessor qp, List<QuerySolution> solutions){
        List<String> resultVars = qp.getResultVars();
        for (String var : resultVars)
            System.out.print(var + "\t");
        System.out.println();

        for(QuerySolution solution : solutions){
            for(String var : resultVars){
                try{
                    String line = solution.get(var).toString() + "\t";
                    System.out.print(line);
                } catch (NullPointerException e){
                    System.out.print("<NONE>\t");
                }
            }
            System.out.println();
        }

        System.out.println("Result Size: " + solutions.size());
    }

    public static void printQueryResult(List<List<String>> solutions){
        for(List<String> row : solutions){
            StringBuilder printableLine = new StringBuilder();

            for(String columnEntry : row){
                if(columnEntry != null){
                    printableLine.append(columnEntry).append("\t");
                } else {
                    printableLine.append("<NONE>\t");
                }
            }

            System.out.println(printableLine.substring(0, printableLine.length() - 1));
        }

        System.out.println("Result Size: " + (solutions.size()-1) + "\n");
    }
}
