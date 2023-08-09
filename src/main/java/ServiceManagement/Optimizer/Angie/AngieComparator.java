package ServiceManagement.Optimizer.Angie;

import FunctionStore.Function.FuncInstance;
import FunctionStore.FunctionStore;

import java.util.Comparator;

public class AngieComparator implements Comparator<FuncInstance> {
    @Override
    public int compare(FuncInstance o1, FuncInstance o2) {
        int rt1 = FunctionStore.get(o1.getApiName()).getResponseTime();
        int rt2 = FunctionStore.get(o2.getApiName()).getResponseTime();

        if(rt1 == rt2){
            int rp1 = (int) o1.getResponseProbability() * 100;
            int rp2 = (int) o2.getResponseProbability() * 100;

            return Integer.compare(rp1,rp2);
        } else {
            return Integer.compare(rt1,rt2);
        }
    }
}
