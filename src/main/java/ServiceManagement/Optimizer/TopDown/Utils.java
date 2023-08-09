package ServiceManagement.Optimizer.TopDown;

import Configuration.Utils.UserPreferences;
import FunctionStore.Function.FuncInstance;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.QualityOfService;
import ServiceManagement.Plan.ServicePlan;
import org.apache.jena.atlas.lib.Pair;

import java.util.LinkedList;
import java.util.List;

public class Utils {

    public static List<Pair<FuncInstance,QualityOfService>> getOrderedNamedInstances(String apiName, QueryResult qr, GapMap gapMap, UserPreferences setting){
        List<Pair<FuncInstance, QualityOfService>> list = new LinkedList<>();
        List<FuncInstance> instances = gapMap.getInstances(apiName);

        for(FuncInstance instance : instances){
//            QualityOfService qos = calcQualityImpact(gapMap.getPlan(),instance);
            QualityOfService qos = calcQualityImpact(qr,gapMap,instance); // TODO IMPORTANT
            list.add(new Pair<>(instance,qos));
        }

        // Sort instances according to user preferences
        sortInstances(list,setting);

        return list;
    }

    public static void sortInstances(List<Pair<FuncInstance, QualityOfService>> list, UserPreferences setting){
        list.sort((p1, p2) -> -1 * QualityOfService.compareQuality(p1.getRight(),p2.getRight(),setting));
    }

//    public static QualityOfService calcQualityImpact(ServicePlan optPlan, FuncInstance instance){
    public static QualityOfService calcQualityImpact(QueryResult qr, GapMap gapMap, FuncInstance instance){
        ServicePlan clonedPlan = gapMap.getPlan().clone();

        LinkedList<FuncInstance> tmp = new LinkedList<>();
        tmp.add(instance);
        clonedPlan.removeInstances(qr,gapMap,tmp);

        return clonedPlan.getQos();
    }
}
