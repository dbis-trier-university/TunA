package Tests;

import Configuration.Configuration;
import Configuration.Database.DbObject;
import QueryManagement.Processor.QueryProcessor;
import org.apache.jena.query.QuerySolution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SparqlFile {
    static String file = "./res/testcases/query.sparql";
    static String label = "sample_lmdb_gaps";

    public static void main(String[] args) {
        Configuration config = Configuration.getInstance();
        DbObject dbObject = config.getDbObject(label);

        System.out.println("DB Path: " + dbObject.getIndexPath());
        runQueries(file,dbObject.getIndexPath());
    }

    public static void runQueries(String file, String url){
        String queryStr;
        try {
            queryStr = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long start = System.currentTimeMillis();
        QueryProcessor qp = new QueryProcessor(queryStr,url);
        List<QuerySolution> result = qp.query();
        qp.close();
        long end = System.currentTimeMillis();
        double time = (end - start)/1000.0;

        TestPrinter.printQueryResult(qp,result);
        System.out.printf("Done after %.2f seconds.",time);
    }
}
