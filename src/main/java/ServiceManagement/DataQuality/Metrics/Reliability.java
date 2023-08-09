package ServiceManagement.DataQuality.Metrics;

import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Utils.Alignment;
import ServiceManagement.Plan.ServicePlan;
import org.apache.jena.atlas.lib.Pair;

import java.util.*;

public class Reliability {
    // Map<QoS-ID,Map<Row:Relation,Reliability>
    private static Map<Integer,Map<String,Double>> reliabilityMap = new HashMap<>();

    public static double computeReliability(int id, List<FuncInstance> instances){
        return computeReliability(id,instances,null);
    }

    public static double computeReliability(int id, List<FuncInstance> instances, List<FuncInstance> remInstances){
        double reliability = 0;

        Map<Pair<Integer,String>, List<FuncInstance>> gapSets = ServicePlan.computeGapMap(instances);
        for(Map.Entry<Pair<Integer,String>,List<FuncInstance>> entry : gapSets.entrySet()){
            reliability += computeReliability(id,entry.getKey().getRight(),entry.getValue(),remInstances);
        }
        reliability = reliability / gapSets.size();

        return Utils.round(reliability);
    }

    private static double computeReliability(int id, String relation,
                                             List<FuncInstance> instanceList,
                                             List<FuncInstance> remInstances)
    {
        if(instanceList.size() > 1){
            return sumPoissonBinomialDistribution(id,relation,instanceList,remInstances);
        } else {
            FuncInstance instance = instanceList.iterator().next();
            Alignment alignment = instance.getSelectedAlignment(relation);
            if(alignment != null) return alignment.getReliability();
            else throw new NullPointerException("No alignment for " + relation + " in " + instance + " found.");
        }
    }

    private static double sumPoissonBinomialDistribution(int id, String relation,
                                                         List<FuncInstance> instances, List<FuncInstance> remInstances)
    {
        double result = 0;

        String colKey = instances.get(0).getRowId() + ":" + relation;
        if(!Utils.recomputationNeeded(id,remInstances,relation)
                && reliabilityMap.containsKey(id) && reliabilityMap.get(id).containsKey(colKey))
        {
            result = reliabilityMap.get(id).get(colKey);
        } else {
            double[] pk = sumPoissonBinomialDistribution(relation, instances, Integer.MAX_VALUE,2);

            for (int i = 1; i < pk.length; i++) {
                result += pk[i];
            }

            if(id > 0){
                Map<String,Double> tmp;
                if(reliabilityMap.containsKey(id)) tmp = reliabilityMap.get(id);
                else tmp = new HashMap<>();

                tmp.put(colKey,result);
                reliabilityMap.put(id,tmp);
            }
        }

        return result;
    }

    private static double[] sumPoissonBinomialDistribution(String relation,
                                                         List<FuncInstance> instances,
                                                         int maxN, double maxCumPr)
    {
        List<Double> w = new LinkedList<>();
        for(FuncInstance instance : instances){
            double rel = instance.getSelectedAlignment(relation).getReliability();
            w.add(rel/(1-rel));
        }
        w.sort(Double::compareTo);
        Collections.reverse(w);

        int n = instances.size();
        int mN = Math.min(maxN,n);

        double z = 1;
        for (Double d : w) {
            z = z / (d + 1.0);
        }

        double[] r = new double[n+1];
        Arrays.fill(r,1d);

        r[n] = z;

        int i = 1;
        int j = 0;
        int k = 0;
        int m = 0;
        double s = 0;
        var cumPr = r[n];

        while(cumPr < maxCumPr && i <= mN) {
            s = 0;
            j = 0;
            m = n - i;
            k = i - 1;

            while (j <= m) {
                s += r[j] * w.get(k + j);
                r[j] = s;
                j += 1;
            }

            r[j - 1] *= z;
            cumPr += r[j - 1];

            i += 1;
        }

        return finalizeR(r, i, n);
    }

    private static double[] finalizeR(double[] r, int i, int n){
        if (i <= n) {
            double[] smallerR = new double[i];
            System.arraycopy(r,n - i + 1,smallerR,0,i);
            return reverse(smallerR);
        } else {
            return reverse(r);
        }
    }

    private static double[] reverse(double[] a){
        int n = a.length / 2;
        int i = 0;
        int j;
        double t;

        while (i <= n) {
            j = a.length - i - 1;
            t = a[i];
            a[i] = a[j];
            a[j] = t;
            i += 1;
        }

        return a;
    }
}
