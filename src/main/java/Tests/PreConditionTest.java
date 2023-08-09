package Tests;

import Configuration.Configuration;
import Configuration.Database.DbObject;
import Configuration.Utils.Setting;
import Configuration.Utils.TestSetting;
import FunctionStore.FunctionStore;
import QueryManagement.Processor.TunableQueryProcessor;
import Utils.ConsoleLogo;
import Utils.Utils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class PreConditionTest {
    static boolean printQueries = false;
    static String scenario = "checked";
    static TestSetting testConfig = Configuration.getInstance().getTestSettings(Setting.PRECONDITIONS,scenario);

    public static void main(String[] args) {
        ConsoleLogo.printTestLogo();

        Configuration config = Configuration.getInstance();
        FunctionStore functionStore = FunctionStore.getInstance(testConfig.getFunctionStore());
        DbObject dbObject = config.getDbObject(scenario);

        String path = testConfig.getQueryPath();
        System.out.println("Testing Queries in folder " + path + "\n");

        runQueries(path,dbObject.getIndexPath(),functionStore);

        System.out.println("Done..");
    }

    public static void runQueries(String dir, String url, FunctionStore functionStore){
        List<String> failedQueries = new LinkedList<>();
        int failed = 0;

        for(String name : Utils.getFileNames(dir)){
            String orgQueryStr;
            String transQueryString;
            try {
                orgQueryStr = Files.readString(Paths.get(dir + name), StandardCharsets.UTF_8);
                transQueryString = Files.readString(Paths.get(testConfig.getResultPath() + name), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            TunableQueryProcessor tqp = new TunableQueryProcessor(orgQueryStr,url,functionStore,null);

            Query transQuery = QueryFactory.create(transQueryString);

            Op tOp = tqp.getTQuery().getPreconditionAlgebra();
            Op mOp = Algebra.compile(transQuery);
            boolean validExtension = tOp.equals(mOp);

            if(!validExtension){
                failedQueries.add(name);
                failed++;
            }

            if(printQueries || !validExtension){
                TestPrinter.printPreconditionQueries(tqp,transQueryString,name);
            }

            tqp.close();
        }

        TestPrinter.printStatus(failedQueries,dir,failed);
    }
}
