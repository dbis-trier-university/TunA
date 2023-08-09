package Tests;

import Configuration.Configuration;
import Configuration.Database.DbObject;
import Configuration.Utils.UserPreferences;
import FunctionStore.FunctionStore;
import QueryManagement.Processor.TunableQueryProcessor;
import ServiceManagement.Optimizer.Optimizer;
import org.apache.jena.atlas.lib.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class TunaFile {
    static String file = "./res/testcases/query.sparql";
    static String fsLabel = "evalF3";
    static String dbLabel = "sample_lmdb_gaps";

    public static void main(String[] args) {
        Configuration config = Configuration.getInstance();
        DbObject dbObject = config.getDbObject(dbLabel);

        System.out.println("DB Path: " + dbObject.getIndexPath() + "\n");
        runQueries(file,dbObject.getIndexPath());
    }

    public static void runQueries(String file, String url){
        String queryStr;
        try {
            queryStr = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Pair<String,Double>> orderedSettings = new LinkedList<>();
        orderedSettings.add(new Pair<>("Time",400000000000.0));
//        orderedSettings.add(new Pair<>("Time",2500.0));
        orderedSettings.add(new Pair<>("Coverage",1.0));
        orderedSettings.add(new Pair<>("Reliability",0.9));
        orderedSettings.add(new Pair<>("Calls",2000.0));
        UserPreferences setting = new UserPreferences(orderedSettings);

        String fPath = Configuration.getInstance().getFunctionStorePath() + fsLabel + "/";
        FunctionStore functionStore = FunctionStore.getInstance(fPath);

        long start = System.currentTimeMillis();
        TunableQueryProcessor tqp = new TunableQueryProcessor(queryStr,url,functionStore,setting);
        tqp.OPT = Optimizer.ROUNDROBIN;
        List<List<String>> solutions = tqp.queryAsList();
        tqp.close();
        long end = System.currentTimeMillis();
        double time = (end - start)/1000.0;

        TestPrinter.printQueryResult(solutions);
        System.out.printf("Done after %.2f seconds.",time);
    }
}
