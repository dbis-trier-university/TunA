package Tests;

import Configuration.Configuration;
import Configuration.Database.DbObject;
import Configuration.Utils.Setting;
import Configuration.Utils.TestSetting;
import Configuration.Utils.UserPreferences;
import FunctionStore.FunctionStore;
import QueryManagement.Processor.TunableQueryProcessor;
import Utils.ConsoleLogo;
import Utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class IntegrationTest {
    static String scenario = "mediumFederation";
    static boolean printResults = true;
    static TestSetting testConfig = Configuration.getInstance().getTestSettings(Setting.INTEGRATIONS,scenario);

    public static void main(String[] args) {
        ConsoleLogo.printTestLogo();

        Configuration config = Configuration.getInstance();
        FunctionStore functionStore = FunctionStore.getInstance(testConfig.getFunctionStore());
        DbObject dbObject = config.getDbObject(testConfig.getDbLabel());

        String path = testConfig.getQueryPath();
        System.out.println("Testing Queries in folder " + path + "\n");

        runQueries(path, dbObject.getIndexPath(), functionStore);

        System.out.println("Done..");
    }

    public static void runQueries(String dir, String url, FunctionStore functionStore) {
        List<String> failedQueries = new LinkedList<>();

        for (String name : Utils.getFileNames(dir)) {
            String orgQueryStr;
            try {
                orgQueryStr = Files.readString(Paths.get(dir + name), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            long start = System.currentTimeMillis();
            UserPreferences setting = testConfig.getSetting();
            TunableQueryProcessor tqp = new TunableQueryProcessor(orgQueryStr,url,functionStore,setting);
            tqp.OPT = testConfig.getProcessor();
            List<List<String>> solutions = tqp.queryAsList();
            tqp.close();
            long end = System.currentTimeMillis();
            long time = end - start;

            List<String> validSolutions = loadValidResult(name);

            boolean isCorrect = checkResults(solutions,validSolutions);
            if (!isCorrect) failedQueries.add(name);
            if (printResults) {
                System.out.println(name);
                System.out.println("------------------------");
                TestPrinter.printQueryResult(solutions);
            }

            System.out.println("Done after " + time + " ms.");
        }

        TestPrinter.printStatus(failedQueries, dir, failedQueries.size());
    }

    private static List<String> loadValidResult(String name) {
        List<String> validResults;
        try {
            String tmpName = name.replace("sparql", "txt");
            validResults = Files.lines(Paths.get(testConfig.getResultPath() + tmpName)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return validResults;
    }

    private static boolean checkResults(List<List<String>> results, List<String> validResults) {
        if (results.size() != validResults.size()) return false;

        for (int i = 0; i < results.size(); i++) {
            String resultLine = buildValidLine(results.get(i));
            String validLine = validResults.get(i);

            if (!validLine.equals(resultLine)) return false;
        }

        return true;
    }

    private static String buildValidLine(List<String> solution) {
        String resultLine = "";

        StringBuilder tmpLine = new StringBuilder();
        for(String entry : solution){
            if(entry != null){
                tmpLine.append(entry).append("\t");
            } else {
                tmpLine.append("<NONE>\t");
            }
        }
        if (tmpLine.toString().endsWith("\t")) resultLine = tmpLine.substring(0, tmpLine.length() - 1);

        return resultLine;
    }

}
