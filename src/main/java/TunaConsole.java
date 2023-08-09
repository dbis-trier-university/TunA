import Configuration.Configuration;
import Configuration.Database.DbObject;
import Configuration.Utils.UserPreferences;
import FunctionStore.FunctionStore;
import QueryManagement.Processor.TunableQueryProcessor;
import ServiceManagement.Optimizer.Optimizer;
import Tests.TestPrinter;
import Utils.ConsoleLogo;
import org.apache.jena.atlas.lib.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class TunaConsole {
    static String queryPath = "res/testcases/";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ConsoleLogo.printLogo();

        System.out.println("-----------------------------");
        System.out.println("Data Management");
        System.out.println("-----------------------------");
        System.out.print("Select Database: ");
        String dbLabel = sc.nextLine();

        System.out.print("Select Function Store: ");
        String fsLabel = sc.nextLine();

        Configuration config = Configuration.getInstance();
        DbObject dbObject = config.getDbObject(dbLabel);
        String path = dbObject.getIndexPath();
        String fPath = Configuration.getInstance().getFunctionStorePath() + fsLabel + "/";
        FunctionStore functionStore = FunctionStore.getInstance(fPath);

        System.out.print("Query File: ");
        String fileName = sc.nextLine();
        System.out.println();

        String queryStr = null;
        try {
            queryStr = Files.readString(Paths.get(queryPath + fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Pair<String,Double>> preferenceList = new LinkedList<>();
        System.out.println("-----------------------------");
        System.out.println("User Preferences");
        System.out.println("-----------------------------");
        System.out.print("Time Limit (ms): ");
        preferenceList.add(new Pair<>("Time",Double.parseDouble(sc.nextLine())));
        System.out.print("Minimum Coverage: ");
        preferenceList.add(new Pair<>("Coverage",Double.parseDouble(sc.nextLine())));
        System.out.print("Minimum Reliability: ");
        preferenceList.add(new Pair<>("Reliability",Double.parseDouble(sc.nextLine())));
        UserPreferences preferences = new UserPreferences(preferenceList);

        long start = System.currentTimeMillis();
        TunableQueryProcessor tqp = new TunableQueryProcessor(queryStr,path,functionStore,preferences);
        tqp.OPT = Optimizer.GOLD_STANDARD; //TODO move option to config file
        List<List<String>> solutions = tqp.queryAsList();
        tqp.close();
        long end = System.currentTimeMillis();
        double time = (end - start)/1000.0;

        System.out.println("-----------------------------");
        TestPrinter.printQueryResult(solutions);
        System.out.printf("Done after %.2f seconds.",time);
    }
}
