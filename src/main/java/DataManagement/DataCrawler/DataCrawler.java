package DataManagement.DataCrawler;

import FunctionStore.Function.FuncInstance;
import QueryManagement.Query.Result.GapMap;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;
import java.util.concurrent.*;

public class DataCrawler {
    public static List<Pair<FuncInstance,HttpResponse>> call(GapMap gapMap){

        long start = System.currentTimeMillis();
        Map<String,List<FuncInstance>> map = convert(gapMap);
        List<Callable<List<Pair<FuncInstance,HttpResponse>>>> threads = schedule(map);
        List<Pair<FuncInstance,HttpResponse>> responseList = execute(threads);
        long end = System.currentTimeMillis();
        long result = end - start;
        System.out.println("Crawl Time: " + result);

        return responseList;
    }

    private static Map<String,List<FuncInstance>> convert (GapMap gapMap){
        Map<String,List<FuncInstance>> map = new HashMap<>();

        for(Set<FuncInstance> parallelCalls : gapMap.getOptPlan().getAsList()){
            for(FuncInstance instance : parallelCalls){

                if(map.containsKey(instance.getApiName())){
                    List<FuncInstance> list = map.get(instance.getApiName());
                    list.add(instance);
                    map.put(instance.getApiName(),list);
                } else {
                    List<FuncInstance> list = new LinkedList<>();
                    list.add(instance);
                    map.put(instance.getApiName(),list);
                }
            }
        }

        return map;
    }

    // TODO refactoring
    private static List<Callable<List<Pair<FuncInstance,HttpResponse>>>> schedule(Map<String,List<FuncInstance>> map){
        List<Callable<List<Pair<FuncInstance,HttpResponse>>>> threads = new LinkedList<>();
        for(Map.Entry<String,List<FuncInstance>> entry : map.entrySet()){
            Callable<List<Pair<FuncInstance,HttpResponse>>> c = new Callable<>() {
                @Override
                public List<Pair<FuncInstance,HttpResponse>> call() {
                    List<Pair<FuncInstance,HttpResponse>> test = new LinkedList<>();

                    for(FuncInstance instance : entry.getValue()){
                        test.add(new Pair<>(instance,HttpHandler.sendGetRequest(instance.getUrl(),instance.getService().getTimeout(),0))); //TODO
                    }

                    return test;
                }
            };
            threads.add(c);
        }

        return threads;
    }

    private static List<Pair<FuncInstance,HttpResponse>> execute(List<Callable<List<Pair<FuncInstance,HttpResponse>>>> threads){
        List<Pair<FuncInstance,HttpResponse>> responseList = new LinkedList<>();

        try {
            ExecutorService pool = Executors.newCachedThreadPool();
            List<Future<List<Pair<FuncInstance,HttpResponse>>>> results = pool.invokeAll(threads);

            for(Future<List<Pair<FuncInstance,HttpResponse>>> result : results){
                responseList.addAll(result.get());
            }

            pool.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return responseList;
    }
}
