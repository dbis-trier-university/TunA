package QueryManagement.Processor;

import Configuration.Configuration;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb2.TDB2Factory;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is used to implement a processor that is able to process simple SPARQL queries. It is basically just
 * a wrapper around the apache jena methods to keep it more simplistic.
 */
public class QueryProcessor {
    static final Logger logger = Configuration.getInstance().getLogger();
    boolean remoteEndpoint;
    Query query;
    QueryExecution queryExecution;
    Dataset dataset;
    Model model;

    public QueryProcessor(String location){
        remoteEndpoint = location.startsWith("http");
    }

    /**
     * Creates a simple SPARQL processor which can be used to execute simple SPARQL queries.
     *
     * @param queryStr String representation of the query.
     * @param location String representation of the SPARQL endpoint (local files or remote URL).
     */
    public QueryProcessor(String queryStr, String location){
        remoteEndpoint = location.startsWith("http");

        if(remoteEndpoint) prepareRemoteQuery(queryStr,location);
        else prepareLocalQuery(queryStr,location);
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    /**
     * Execute a simple SPARQL select query
     *
     * @return A result set containing the requested data.
     */
    public List<List<String>> queryAsList() {
        this.queryExecution = QueryExecutionFactory.create(query,model);
        if(dataset != null) dataset.begin(ReadWrite.READ);

        return transform(ResultSetFormatter.toList(this.queryExecution.execSelect()));
    }

    public List<QuerySolution> query() {
        this.queryExecution = QueryExecutionFactory.create(query,model);
        if(dataset != null) dataset.begin(ReadWrite.READ);

        return ResultSetFormatter.toList(this.queryExecution.execSelect());
    }

    /**
     * This method is used to close the stream to the RDF dataset. Otherwise, no other processors could access
     * the dataset without throwing an exception.
     */
    public void close(){
        if(this.queryExecution != null) this.queryExecution.close();
        if(this.dataset != null) {
            this.dataset.end();
            this.dataset.close();
        }
    }

    /**
     * Extracts all select (or result) variables used in a SPARQL query.
     *
     * @return A list containing all select (or result) variables used in a SPARQL query.
     */
    public List<String> getResultVars(){
        return this.query.getResultVars();
    }

    /**
     * @return The used query object of a query processor object.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * @return the used dataset of a query processor object.
     */
    public Dataset getDataset() {
        return dataset;
    }

    /**
     * @return the used model of a query processor object.
     */
    public Model getModel() {
        return model;
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    /**
     * This method is a helper method of the constructor. It prepares all information in order to execute a
     * SPARQL query against a remote SPARQL endpoint.
     *
     * @param queryStr String representation of the query.
     * @param location String representation of the SPARQL endpoint (local files or remote URL).
     */
    void prepareRemoteQuery(String queryStr, String location){
        this.query = QueryFactory.create(queryStr);
        this.queryExecution = QueryExecutionFactory.sparqlService(location, this.query);
    }

    /**
     * This method is a helper method of the constructor. It prepares all information in order to execute a
     * SPARQL query against a local triple file or triple database index.
     *
     * @param queryStr String representation of the query.
     * @param location String representation of the SPARQL endpoint (local files or remote URL).
     */
    void prepareLocalQuery(String queryStr, String location){
        boolean ttlFile = location.endsWith(".ttl") || location.endsWith(".nt") || location.endsWith(".n3");

        if(ttlFile){
            this.model = ModelFactory.createDefaultModel();
            this.model.read(location);
        } else {
            this.query = QueryFactory.create(queryStr);
            this.dataset = TDB2Factory.connectDataset(location);
            this.model = this.dataset.getDefaultModel();
        }

        this.query = QueryFactory.create(queryStr);
    }

    private List<List<String>> transform(List<QuerySolution> solutions){
        List<List<String>> result = new LinkedList<>();

        List<String> header = new LinkedList<>(query.getResultVars());
        result.add(header);

        for(QuerySolution solution : solutions){
            List<String> row = new LinkedList<>();
            for(String var : query.getResultVars()){
                row.add(solution.get(var).toString());
            }
            result.add(row);
        }

        return result;
    }
}
