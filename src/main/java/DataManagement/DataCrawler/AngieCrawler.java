package DataManagement.DataCrawler;

import FunctionStore.Function.FuncInstance;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.MissingResult;
import ServiceManagement.Optimizer.Angie.AngieComparator;
import org.apache.jena.atlas.lib.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AngieCrawler {

    public static List<Pair<FuncInstance,HttpResponse>> call(GapMap gapMap,int maxCalls, int maxTime){
        List<Pair<FuncInstance,HttpResponse>> responseList = new LinkedList<>();

        long time = 0;
        int calls = 0;

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapMap.entrySet()){
            List<FuncInstance> fList = entry.getValue();
            fList.sort(new AngieComparator());

            for(FuncInstance instance : fList){
                int timeout = instance.getService().getTimeout();

                long start = System.currentTimeMillis();
                HttpResponse response = HttpHandler.sendGetRequest(instance.getUrl(),timeout,0);
                responseList.add(new Pair<>(instance,response));
                long end = System.currentTimeMillis();
                long responseTime = end - start;

                time += responseTime + timeout;
                calls++;

                if(time >= maxTime || calls >= maxCalls) return responseList;
                if(response != null) {
                    break;
                }
            }
        }

        return responseList;
    }
}
