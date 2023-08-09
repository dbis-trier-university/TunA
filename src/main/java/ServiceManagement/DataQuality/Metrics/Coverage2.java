package ServiceManagement.DataQuality.Metrics;

import FunctionStore.Function.FuncInstance;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.MissingResult;
import org.apache.jena.atlas.lib.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Coverage2 {
    public static double computeCoverage(GapMap gapMap, int size){
        double coverage = 0;

        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> gapEntry : gapMap.entrySet()){
            if(gapEntry.getValue().size() > 1) {
                coverage += computeList(gapEntry.getValue().toArray());
            } else if(gapEntry.getValue().size() == 1){
                coverage += FunctionStore.get(gapEntry.getValue().get(0).getApiName()).getResponseProbability();
            }
        }
        coverage = coverage / size;

        return Utils.round(coverage);
    }

    public static double computeCoverage(Map<MissingResult,List<FuncInstance>> map, int size){
        double coverage = 0;

        for(Map.Entry<MissingResult,List<FuncInstance>> gapEntry : map.entrySet()){
            if(gapEntry.getValue().size() > 1) {
                coverage += computeList(gapEntry.getValue().toArray());
            } else if(gapEntry.getValue().size() == 1){
                coverage += FunctionStore.get(gapEntry.getValue().get(0).getApiName()).getResponseProbability();
            }
        }
        coverage = coverage / size;

        return Utils.round(coverage);
    }

    public static double computeCoverage(List<Set<FuncInstance>> list, int size){
        double coverage = 0;

        for(Set<FuncInstance> gap : list){
            if(gap.size() > 1) {
                coverage += computeList(gap.toArray());
            } else if(gap.size() == 1){
                for(FuncInstance inst : gap) coverage += FunctionStore.get(inst.getApiName()).getResponseProbability();
            }
        }
        coverage = coverage/size;

        return Utils.round(coverage);
    }

    public static double computeList(Object a[]) {
        double odd = 0;
        double even = 0;
        long counter = 0;
        int j = 0;
        double p = 1;
        long pow_set_size = (1L << a.length);

        // Run from counter 000..0 to 111..1
        for (counter = 1; counter < pow_set_size;
             counter++) {
            p = 1;
            for (j = 0; j < a.length; j++) {
                // Check if jth bit in the
                // counter is set If set
                // then print jth element from set
                if ((counter & (1L << j)) > 0) {
                    p *= ((FuncInstance) a[j]).getResponseProbability();
                }
            }

            // if set bits is odd, then add to
            // the number of multiples
            if (Long.bitCount(counter) % 2 == 1) {
                odd += p;
            }
            else {
                even += p;
            }
        }

        return odd - even;
    }

}
