package Configuration.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestConfiguration {
    private Map<String, List<TestSetting>> testMap;

    public TestConfiguration(){
        this.testMap = new HashMap<>();
    }

    public void put(String key,List<TestSetting> entryList){
        this.testMap.put(key,entryList);
    }

    public TestSetting get(String category, String scenarioName){
        List<TestSetting> entryList = this.testMap.get(category);

        for(TestSetting entry : entryList){
            if(entry.getLabel().equalsIgnoreCase(scenarioName)) return entry;
        }

        return null;
    }
}
