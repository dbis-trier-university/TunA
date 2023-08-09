package QueryManagement.Processor;

import Configuration.Utils.UserPreferences;
import DataManagement.DataCrawler.AngieCrawler;
import DataManagement.DataCrawler.DataCrawler;
import DataManagement.DataCrawler.HttpResponse;
import DataManagement.Integration.VotingBasedIntegrator;
import DataManagement.Refinement.Refiner;
import FunctionStore.Function.FuncInstance;
import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Algebra.Manipulation.OptionalManipulator;
import QueryManagement.Query.Algebra.Manipulation.PreConditionManipulator;
import QueryManagement.Query.Algebra.Manipulation.SubtractionManipulator;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.QueryResult;
import QueryManagement.Query.TunableQuery;
import ServiceManagement.Optimizer.Angie.AngieOptimizer;
import ServiceManagement.Optimizer.GoldStandard.GSOptimizer;
import ServiceManagement.Optimizer.Optimizer;
import ServiceManagement.Optimizer.PlanOptimizer;
import ServiceManagement.Optimizer.Random.ListOptimizer;
import ServiceManagement.Optimizer.Random.SingleOptimizer;
import ServiceManagement.Optimizer.Random.SmartSingleOptimizer;
import ServiceManagement.Optimizer.RoundRobin.RROptimizer;
import ServiceManagement.Optimizer.TopDown.TdListOptimizer;
import ServiceManagement.Optimizer.TopDown.TdSingleOptimizer;
import ServiceManagement.Plan.OptServicePlan;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.algebra.OpAsQuery;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to implement a processor that is able to tune a query according to
 * different aspects (e.g. completeness, accuracy, currency, etc.).
 */
public class TunableQueryProcessor extends QueryProcessor {
    public Optimizer OPT;
    private TunableQuery tQuery;
    private GapMap gapMap;
    private UserPreferences setting;

    /**
     * Creates a TunableQueryProcessor object, i.e. the given query will be extended according to the
     * rules of QueryManipulation.
     *
     * @param queryStr String representation of the query.
     * @param location String representation of the SPARQL endpoint (local files or remote URL).
     * @param functionStore This function store object contains all functions specified in the function store location.
     */
    public TunableQueryProcessor(String queryStr, String location, FunctionStore functionStore, UserPreferences setting) {
        super(location);
        this.tQuery = new TunableQuery(queryStr,functionStore);
        this.setting = setting;

        extendQuery(this.tQuery);

        queryStr = OpAsQuery.asQuery(this.tQuery.getCurrentAlgebra()).toString();
        if(this.remoteEndpoint) prepareRemoteQuery(queryStr,location);
        else prepareLocalQuery(queryStr,location);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    /**
     * Returns a tunable query object that is used by this processor.
     *
     * @return A tunable query object.
     */
    public TunableQuery getTQuery() {
        return tQuery;
    }

    public OptServicePlan getOptServicePlan() {
        return this.gapMap.getOptPlan();
    }

    public UserPreferences getSetting() {
        return setting;
    }

    @Override
    public List<List<String>> queryAsList() {
        long start = System.currentTimeMillis();

        String queryString = OpAsQuery.asQuery(tQuery.getCurrentAlgebra()).toString();
        logger.finer("Start querying:\n" + queryString);
        List<QuerySolution> localSolutions = super.query();
        logger.finer("Querying was successful");

        List<List<String>> integratedResult = new LinkedList<>();
        if(this.setting != null){
            long qStart = System.currentTimeMillis();
            QueryResult queryResult = new QueryResult(tQuery,localSolutions);
            long qEnd = System.currentTimeMillis();
            long queryTime = qEnd - qStart;
            integratedResult = integrateExternalData(queryResult,localSolutions,queryTime);
        }

        long end = System.currentTimeMillis();
        long time = end - start;
        logger.finer("Done optimizing and integrating after " + time + "ms.");

        return integratedResult;
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private void extendQuery(TunableQuery tunableQuery){
        logger.info("Remove subtractions, groupings, havings and orders");
        SubtractionManipulator.removeRefinements(tunableQuery);

        logger.info("Extending original query by adding pre-conditions");
        PreConditionManipulator.addPreConditions(tunableQuery);

        logger.info("Transform selected triples to optionals");
        OptionalManipulator.transformSelectTriples(tunableQuery);

        tunableQuery.computeResultVarsMap();
    }

    private List<List<String>> integrateExternalData(QueryResult qResult, List<QuerySolution> solutions, long QTime){
        this.gapMap = identifyServicePlan(qResult,QTime);

        List<List<String>> result;
        if(gapMap != null){
            // Call Web APIs
            List<Pair<FuncInstance,HttpResponse>> responses;
            if(this.OPT == Optimizer.ANGIE){
                int maxCalls = (int) this.setting.get("Calls");
                int time = (int) this.setting.get("Time");
                responses = AngieCrawler.call(this.gapMap,maxCalls,time);
            } else {
                responses = DataCrawler.call(this.gapMap);
            }
            System.out.println("Calls: " + responses.size());

            // Integrate data into the query result
            VotingBasedIntegrator integrator = new VotingBasedIntegrator(this,solutions,this.gapMap,qResult.getResultVarsMap(),responses);
            result = integrator.integrate();

            if(this.OPT != Optimizer.ANGIE){
                double untrusted = ((double) integrator.untrustedCounter) / (qResult.solCounter + qResult.noSolCounter);
                double foundResults = (qResult.solCounter + (integrator.solutionMap.size() * gapMap.getPlan().getQos().get("Coverage")));
                System.out.println("Untrusted Results: " + integrator.untrustedCounter + " (" + untrusted + ")");
                System.out.println("Est. Coverage: " + (foundResults / (qResult.solCounter + qResult.noSolCounter)) );
                System.out.println("Coverage: " + ((foundResults - integrator.untrustedCounter) / (qResult.solCounter + qResult.noSolCounter)) );
                System.out.println("Reliability: " + gapMap.getPlan().getQos().getReliability() + "\n");
            }
        } else {
            result = convert(solutions);
        }

        // Refine results (i.e. process group by, order by, etc.)
        result = Refiner.refinement(tQuery,result);

        return result;
    }


    private GapMap identifyServicePlan(QueryResult queryResult,long qTime){
        PlanOptimizer optimizer;
        List<Function> fs = this.tQuery.getFunctionStore();

        switch (OPT){
            case ANGIE -> optimizer = new AngieOptimizer(queryResult, fs, this.setting, qTime);
            case GOLD_STANDARD -> optimizer = new GSOptimizer(queryResult, fs, this.setting);
            case TD_SINGLE -> optimizer = new TdSingleOptimizer(queryResult, fs, this.setting, qTime);
            case TD_LIST -> optimizer = new TdListOptimizer(queryResult, fs, this.setting, qTime);
            case RDN_SINGLE -> optimizer = new SingleOptimizer(queryResult, fs, this.setting, qTime);
            case RDN_SMART_SINGLE -> optimizer = new SmartSingleOptimizer(queryResult, fs, this.setting, qTime);
            case RDN_LIST -> optimizer = new ListOptimizer(queryResult, fs, this.setting, qTime);
            default -> optimizer = new RROptimizer(queryResult, fs, this.setting, qTime); // ROUNDROBIN
        }

        return optimizer.identifyBestPlan();
    }

    private List<List<String>> convert(List<QuerySolution> solutions){
        List<String> resultVars = getTQuery().getOriginalResultVars();
        List<List<String>> resultList = new LinkedList<>();
        resultList.add(resultVars);

        for(QuerySolution solution : solutions){
            List<String> row = new LinkedList<>();
            for(String var : resultVars){
                RDFNode node = solution.get(var);
                if(node != null) row.add(node.toString());
                else row.add(null);
            }
            resultList.add(row);
        }

        return resultList;
    }

}
