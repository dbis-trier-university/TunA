package Configuration;

import Configuration.Database.DbObject;
import Configuration.Service.Parameter;
import Configuration.Service.ServiceObject;
import Configuration.Utils.Setting;
import Configuration.Utils.TestConfiguration;
import Configuration.Utils.TestSetting;
import Configuration.Utils.UserPreferences;
import org.apache.jena.atlas.lib.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {
    private static final Path configPath = Path.of("config.json");
    private static final Configuration config = new Configuration();

    private String functionStorePath, secretsPath;
    private List<DbObject> databases;
    private List<ServiceObject> apis;
    private Map<String,String> apiTokenMap;
    private TestConfiguration testSettings;
    private Logger logger;

    /*******************************************************************************************************************
     * Public Methods
     ******************************************************************************************************************/

    public static Configuration getInstance(){
        return config;
    }

    /*******************************************************************************************************************
     * Getter and Setter
     ******************************************************************************************************************/

    public String getFunctionStorePath() {
        return functionStorePath;
    }

    public DbObject getDbObject(String label){
        for(DbObject obj : this.databases){
            if(obj.getLabel().equalsIgnoreCase(label)) return obj;
        }

        return null;
    }

    public ServiceObject getServiceObject(String label){
        for(ServiceObject obj : this.apis){
            if(obj.getLabel().equals(label)) return obj;
        }

        return null;
    }

    public TestSetting getTestSettings(Setting setting, String scenarioName){
        String category;
        switch (setting){
            case PRECONDITIONS -> category = "preconditions";
            case OPTIONALS -> category = "optionals";
            case SERVICEPLANS -> category = "serviceplans";
            default -> category = "integrations";
        }
        return this.testSettings.get(category,scenarioName);
    }

    public Logger getLogger() {
        return logger;
    }


    /*******************************************************************************************************************
    * Private Methods
     ******************************************************************************************************************/

    private Configuration(){
        try {
            String content = Files.readString(configPath);
            JSONObject obj = new JSONObject(content);

            createPathsInformation(obj);
            createDatabases(obj);
            createSecrets();
            createApis(obj);
            createLogger(obj);
            createTestSettings(obj);
        } catch (IOException e) {
            System.out.println("Config folder not found..");
        }
    }

    private void createPathsInformation(JSONObject obj){
        this.functionStorePath = obj.getString("functionstore");
        this.secretsPath = obj.getString("secrets");
    }

    private void createDatabases(JSONObject obj){
        JSONArray dbArray = obj.getJSONArray("databases");
        this.databases = new LinkedList<>();

        for (int i = 0; i < dbArray.length(); i++) {
            JSONObject entry = dbArray.getJSONObject(i);
            String label = entry.getString("label");
            String index = entry.getString("index");
            String source = entry.getString("source");

            this.databases.add(new DbObject(label,index,source));
        }
    }

    private void createApis(JSONObject obj){
        JSONArray apiArray = obj.getJSONArray("apis");
        this.apis = new LinkedList<>();

        for (int i = 0; i < apiArray.length(); i++) {
            JSONObject apiEntry = apiArray.getJSONObject(i);

            String label = apiEntry.getString("label");
            String name = apiEntry.getString("name");
            String url = apiEntry.getString("url");
            String format = apiEntry.getString("format");
            int timeout = apiEntry.getInt("timeout");

            List<Parameter> parameters = new LinkedList<>();
            JSONArray parameterArray = apiEntry.getJSONArray("parameters");
            for (int j = 0; j < parameterArray.length(); j++) {
                JSONObject paraObj = parameterArray.getJSONObject(j);

                String paraName = paraObj.getString("name");
                String type = paraObj.getString("type");
                String relation = paraObj.getString("relation");

                parameters.add(new Parameter(paraName,type,relation));
            }

            this.apis.add(new ServiceObject(label,name,url,format,this.apiTokenMap.get(label),timeout,parameters));
        }
    }

    private void createTestSettings(JSONObject obj){
        obj = obj.getJSONObject("testing");

        JSONArray array = obj.getJSONArray("preconditions");
        List<TestSetting> preconditions = handleTestArray(array);
        this.testSettings = new TestConfiguration();
        this.testSettings.put("preconditions",preconditions);

        array = obj.getJSONArray("optionals");
        List<TestSetting> optionals = handleTestArray(array);
        this.testSettings.put("optionals",optionals);

        array = obj.getJSONArray("serviceplans");
        List<TestSetting> serviceplans = handleTestArray(array);
        this.testSettings.put("serviceplans",serviceplans);

        array = obj.getJSONArray("integrations");
        List<TestSetting> integrations = handleTestArray(array);
        this.testSettings.put("integrations",integrations);
    }

    private List<TestSetting> handleTestArray(JSONArray array){
        List<TestSetting> entryList = new LinkedList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject tmp = array.getJSONObject(i);

            String label = tmp.getString("label");
            String qp = tmp.getString("querypath");
            String rp = tmp.getString("resultpath");
            String db = tmp.getString("database");
            String fs = tmp.getString("functions");
            String pc = tmp.getString("processor");

            UserPreferences us;
            try{
                List<Pair<String,Double>> settingOrder = new LinkedList<>();
                JSONArray settingsArray = tmp.getJSONArray("usersettings");
                for (int j = 0; j < settingsArray.length(); j++) {
                    tmp = settingsArray.getJSONObject(j);
                    settingOrder.add(new Pair<>(tmp.getString("label"),tmp.getDouble("value")));
                }

                us = new UserPreferences(settingOrder);
            } catch (Exception ignore){
                us = null;
            }

            entryList.add(new TestSetting(label,qp,rp,db,fs,pc,us));
        }

        return entryList;
    }

    private void createLogger(JSONObject obj){
        JSONObject loggerObject = obj.getJSONObject("logging");

        String logPath = loggerObject.getString("path");
        this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        this.logger.setLevel(getLogLevel(loggerObject.getString("level")));

        try {
            File directory = new File(logPath);
            if(!directory.exists()) directory.mkdirs();

            String logFilePath = logPath + new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
            File file = new File(logFilePath);
            file.createNewFile();

            FileHandler fileHandler = new FileHandler(logFilePath);
            this.logger.addHandler(fileHandler);

            if(!loggerObject.getBoolean("console")) {
                this.logger.setUseParentHandlers(false);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.logger.info("Creating logger object");
    }

    private void createSecrets() throws IOException {
        String secretContent = Files.readString(Path.of(secretsPath));
        JSONArray secretsArray = new JSONObject(secretContent).getJSONArray("secrets");

        Map<String,String> apiTokenMap = new HashMap<>();
        for (int i = 0; i < secretsArray.length(); i++) {
            JSONObject obj = secretsArray.getJSONObject(i);
            apiTokenMap.put(obj.getString("name"),obj.getString("secret"));
        }

        this.apiTokenMap = apiTokenMap;
    }

    private Level getLogLevel(String level){
        if(level.equalsIgnoreCase("all")) return Level.ALL;
        if(level.equalsIgnoreCase("severe")) return Level.SEVERE;
        if(level.equalsIgnoreCase("warning")) return Level.WARNING;
        if(level.equalsIgnoreCase("info")) return Level.INFO;
        if(level.equalsIgnoreCase("config")) return Level.CONFIG;
        if(level.equalsIgnoreCase("fine")) return Level.FINE;
        if(level.equalsIgnoreCase("finer")) return Level.FINER;
        if(level.equalsIgnoreCase("finest")) return Level.FINEST;

        return Level.OFF;
    }

}
