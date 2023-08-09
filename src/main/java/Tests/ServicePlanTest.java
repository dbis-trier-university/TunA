package Tests;

import Configuration.Configuration;
import Configuration.Database.DbObject;
import Configuration.Utils.Setting;
import Configuration.Utils.TestSetting;
import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import FunctionStore.FunctionStore;
import QueryManagement.Processor.TunableQueryProcessor;
import ServiceManagement.Plan.OptServicePlan;
import Utils.ConsoleLogo;
import Utils.Utils;
import org.apache.jena.query.QuerySolution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class ServicePlanTest {
    static String scenario = "mediumFederation";
    static boolean showPlan = true;
    static TestSetting testConfig = Configuration.getInstance().getTestSettings(Setting.SERVICEPLANS,scenario);

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
        int failed = 0;

        for (String name : Utils.getFileNames(dir)) {
            String orgQueryStr;
            try {
                orgQueryStr = Files.readString(Paths.get(dir + name), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            UserPreferences setting = testConfig.getSetting();
            TunableQueryProcessor tqp = new TunableQueryProcessor(orgQueryStr,url,functionStore,setting);
            tqp.OPT = testConfig.getProcessor();
            List<List<String>> solutions = tqp.queryAsList();
            OptServicePlan optServicePlan = tqp.getOptServicePlan();
            tqp.close();

            List<String> validPlan = loadValidPlan(name);
            if(showPlan) for(FuncInstance instance : optServicePlan.toList()) System.out.println(instance);

            boolean isCorrect = checkPlan(validPlan,optServicePlan);
            if (!isCorrect) {
                failedQueries.add(name);
                failed++;

                List<FuncInstance> optList = optServicePlan.toList();
                if(validPlan.size()-1 != optList.size()) System.out.println("Plan size differs");
                else {
                    for (int i = 0; i < validPlan.size()-1; i++) {
                        boolean result = validPlan.get(i).equals(optList.get(i).getUrl());
                        System.out.println(validPlan.get(i) + " - " + optList.get(i).getUrl() + " - " + result);
                    }

                    String[] qos = validPlan.get(validPlan.size()-1).split(",");
                    System.out.println("Calc: " + optServicePlan.getQos());
                    System.out.println("Test: (" + qos[0] + ", " + qos[1] + ", " + qos[2] + ", " + qos[3] + ")");
                }
            }
        }

        TestPrinter.printStatus(failedQueries, dir, failed);
    }

    public static List<String> loadValidPlan(String name){
        List<String> urls;
        try {
            String tmpName = name.replace("sparql", "txt");
            urls = Files.lines(Paths.get(testConfig.getResultPath() + tmpName)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return urls;
    }

    public static boolean checkPlan(List<String> manualList, OptServicePlan optPlan){
        List<FuncInstance> optList = optPlan.toList();

        if(manualList.size()-1 != optList.size()) return false;

        for (int i = 0; i < manualList.size()-1; i++) {
            if(!manualList.get(i).equals(optList.get(i).getUrl())) return false;
        }

        String[] qos = manualList.get(manualList.size()-1).split(",");
        if(Integer.parseInt(qos[0]) != optPlan.getQos().getCalls()) return false;
        if(Double.parseDouble(qos[1]) != optPlan.getQos().getExecTime()) return false;
        if(Double.parseDouble(qos[2]) != optPlan.getQos().getCoverage()) return false;
        if(Double.parseDouble(qos[3]) != optPlan.getQos().getReliability()) return false;

        return true;
    }

    public static List<String> loadValidResult(String name) {
        List<String> validResults;
        try {
            String tmpName = name.replace("sparql", "txt");
            validResults = Files.lines(Paths.get(testConfig.getResultPath() + tmpName)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return validResults;
    }

    public static boolean checkResults(TunableQueryProcessor tqp, List<QuerySolution> results, List<String> validResults) {
        if (results.size() != validResults.size()) return false;

        List<String> resultVars = tqp.getResultVars();
        for (int i = 0; i < results.size(); i++) {
            String resultLine = buildValidLine(results.get(i), resultVars);
            String validLine = validResults.get(i);

            if (!validLine.equals(resultLine)) return false;
        }

        return true;
    }

    public static String buildValidLine(QuerySolution solution, List<String> resultVars) {
        String resultLine = "";

        StringBuilder tmpLine = new StringBuilder();
        for (String var : resultVars) {
            try {
                tmpLine.append(solution.get(var).toString()).append("\t");
            } catch (NullPointerException e) {
                tmpLine.append("<NONE>\t");
            }
        }
        if (tmpLine.toString().endsWith("\t")) resultLine = tmpLine.substring(0, tmpLine.length() - 1);

        return resultLine;
    }

}
