package ServiceManagement.DataQuality;

import FunctionStore.Function.FuncInstance;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import ServiceManagement.DataQuality.Metrics.Coverage;
import ServiceManagement.DataQuality.Metrics.Coverage2;
import ServiceManagement.DataQuality.Metrics.ExecutionTime;
import ServiceManagement.DataQuality.Metrics.Reliability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataQualityFactory {

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public static QualityOfService create(int id, List<FuncInstance> optPlan){
        return create(id,optPlan,null);
    }

    public static QualityOfService create(int id, List<FuncInstance> optPlan, List<FuncInstance> remInstances){
        int execTime = ExecutionTime.computeExecTime(optPlan);
        double coverage = Coverage.computeCoverage(id,optPlan,remInstances); //TODO too slow
//        double coverage = Coverage2.computeCoverage(); //TODO faster coverage computation
        double reliability = Reliability.computeReliability(id,optPlan,remInstances);

        return new QualityOfService(optPlan.size(),execTime,coverage,reliability);
    }

    public static QualityOfService createFast(int id, QueryResult qr, GapMap gapMap, List<FuncInstance> remInstances){
        int execTime = ExecutionTime.computeExecTime(gapMap.getPlan().toList());
        double orgCoverage = ((double) qr.solCounter) / qr.getLocalSolutions().size();

        double relCoverage = Coverage2.computeCoverage(gapMap,qr.noSolCounter);
        double coverageGrowth = (1-orgCoverage) * relCoverage;
        double coverage = orgCoverage + coverageGrowth ;

        double reliability = Reliability.computeReliability(id,gapMap.getPlan().toList(),remInstances);

        return new QualityOfService(gapMap.getPlan().toList().size(),execTime,coverage,reliability);
    }
}
