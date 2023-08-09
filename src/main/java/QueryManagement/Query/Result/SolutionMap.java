package QueryManagement.Query.Result;

import DataManagement.DataCrawler.HttpResponse;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Utils.Alignment;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;

public class SolutionMap {
    private final Map<Pair<Integer,String>, Map<FuncInstance,String>> solutionsMap;

    public SolutionMap(GapMap gapMap,List<Pair<FuncInstance, HttpResponse>> responses) {
        this.solutionsMap = new HashMap<>();

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> gapEntry : gapMap.entrySet()){
            List<FuncInstance> gapInstances = gapEntry.getValue();

            for(FuncInstance instance : gapInstances){
                HttpResponse response = search(instance,responses);

                if(response != null){
                    List<String> extrData = extract(instance,response,gapEntry.getKey().getRight());

                    Map<FuncInstance,String> solutions;
                    if(solutionsMap.containsKey(gapEntry.getKey())) solutions = solutionsMap.get(gapEntry.getKey());
                    else solutions = new HashMap<>();

                    if(extrData != null && extrData.size() == 1){
                        solutions.put(instance,extrData.get(0));
                        solutionsMap.put(gapEntry.getKey(),solutions);
                    } else if(extrData != null){
                        // TODO how to select the correct value in a list (e.g. of authors)?
                    }
                }
            }
        }
    }

    public int size(){
        return this.solutionsMap.size();
    }

    public static HttpResponse search(FuncInstance instance, List<Pair<FuncInstance, HttpResponse>> responses){
        for(Pair<FuncInstance,HttpResponse> pair : responses){
            if(pair.getLeft().getUrl().equals(instance.getUrl())) return pair.getRight();
        }

        return null;
    }

    public Map<FuncInstance,String> get(int row, String relation){
        for(Map.Entry<Pair<Integer,String>,Map<FuncInstance,String>> entry : this.solutionsMap.entrySet()){
            if(entry.getKey().getLeft() == row
                    && entry.getKey().getRight().equalsIgnoreCase(relation)) return entry.getValue();
        }

        return null;
    }

    private List<String> extract(FuncInstance instance, HttpResponse response, String relation){
        if(response != null && response.getApplicationType().contains("json")){
            Map<String,List<String>> map = convert(Objects.requireNonNull(HttpResponse.convertResponse(response)));
            Alignment a = instance.getSelectedAlignment(relation);

            return map.get(a.getRemoteRelation(relation));
        }

        return null;
    }

    private Map<String,List<String>> convert(Map<String,Object> jsonMap){
        Map<String,List<String>> collapsedMap = new HashMap<>();

        for(Map.Entry<String, Object> jsonEntry : jsonMap.entrySet()){
            String path = jsonEntry.getKey().replaceAll("\\[[0-9][0-9]*\\]", "[*]");

            List<String> list;
            if(collapsedMap.containsKey(path)) list = collapsedMap.get(path);
            else list = new LinkedList<>();
            try{
                list.add((String) jsonEntry.getValue());
            } catch (ClassCastException e){
                list.add(null);
            }

            collapsedMap.put(path,list);
        }

        return collapsedMap;
    }
}
