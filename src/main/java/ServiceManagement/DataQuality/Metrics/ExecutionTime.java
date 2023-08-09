package ServiceManagement.DataQuality.Metrics;

import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExecutionTime {
    public static int computeExecTime(List<FuncInstance> optPlan){
        Map<String,Integer> timeMap = new HashMap<>();

        for(FuncInstance instance : optPlan){
            String apiName = instance.getApiName();
            Function f = FunctionStore.get(apiName);
            int responseTime = f.getResponseTime();

            if(!timeMap.containsKey(instance.getApiName())) {
                timeMap.put(instance.getApiName(),responseTime);
            } else {
                int time = timeMap.get(instance.getApiName());
                int timeout = instance.getService().getTimeout();
                timeMap.put(instance.getApiName(),time + timeout + responseTime);
            }
        }

        int maxExecTime = 0;
        for(Map.Entry<String,Integer> entry : timeMap.entrySet()){
            if(entry.getValue() > maxExecTime) maxExecTime = entry.getValue();
        }

        return maxExecTime;
    }
}
