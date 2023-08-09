package QueryManagement.Query;

import FunctionStore.Function.Function;
import FunctionStore.FunctionStore;
import QueryManagement.Query.Walker.BlockWalker;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;

import java.util.*;

/**
 * The TunableQuery is the core of the Tunable Query Processor. An object of this type stores the
 * original query and the query for each individual transformation step.
 */
public class TunableQuery {
    private final Query originalQuery;
    private Op subtractionAlgebra, preconditionAlgebra, optionalAlgebra, currentAlgebra;
    private Set<TriplePath> triplePaths;
    private Set<Triple> optionalTriples;
    private Map<Function, Node> validFunctions;
    private boolean distinct;
    private int limit;
    private List<String> subjectVars;
    private List<Element> subtractionElements;
    private List<SortCondition> orderByConditions;
    private VarExprList groupByConditions;
    private ExprList filterExpr;
    private Map<Var,Expr> selectExpressions;
    private Map<String,Pair<String,String>> addedVarsMap;
    private Map<String,Pair<String,String>> resultVarsMap;

    /**
     * The constructor will create a query object and uses the function store object to remove all functions that can
     * not be used in the current query.
     *
     * @param queryStr String representation of the query.
     * @param functionStore This function store object contains all functions specified in the function store location.
     */
    public TunableQuery(String queryStr, FunctionStore functionStore) {
        this.originalQuery = QueryFactory.create(queryStr);
        this.currentAlgebra = Algebra.compile(QueryFactory.create(queryStr));
        this.validFunctions = functionStore.getValidFunctionsMap(originalQuery);
        this.triplePaths = extractTriplePaths();

        this.addedVarsMap = new HashMap<>();
        this.resultVarsMap = new HashMap<>();
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    /**
     * Returns the original query before any query transformation steps where done.
     *
     * @return The original query.
     */
    public Query getOriginalQuery(){
        return this.originalQuery;
    }

    public Map<String,Pair<String,String>> getAddedVarsMap(){
        return this.addedVarsMap;
    }

    public Map<String,Pair<String,String>> getResultVarsMap(){
        return this.resultVarsMap;
    }

    /**
     * Returns the first processing step of a tunable query, i.e. the original query without any
     * subtraction blocks (e.g. FILTER or MINUS expressions).
     *
     * @return The original query without any subtraction elements.
     */
    public Op getSubtractionAlgebra() {
        return subtractionAlgebra;
    }

    /**
     * Returns the second processing step of a tunable query, i.e. the subtraction query is extended by
     * pre-condition triples. These are triples containing information (e.g. identifier) that can be used to
     * call Web APIs.
     *
     * @return The subtraction query with additional optional triples.
     */
    public Op getPreconditionAlgebra() {
        return preconditionAlgebra;
    }

    /**
     * Returns the third processing step of a tunable query, i.e. the pre-condition query is further
     * manipulated so that all necessary triples are set to optional triples.
     *
     * @return The pre-condition query with optional select triples.
     */
    public Op getOptionalAlgebra() {
        return optionalAlgebra;
    }

    /**
     * @return The current processing step of a tunable query, e.g original query, subtraction query,
     * pre-condition query or optional query.
     */
    public Op getCurrentAlgebra() {
        return this.currentAlgebra;
    }

    /**
     * This method returns all functions stored in the function store (location) that are helpful for the current query.
     *
     * @return Only functions that are helpful for the current query.
     */
    public Map<Function, Node> getValidFunctionsMap() {
        return validFunctions;
    }

    public List<Function> getFunctionStore(){
        List<Function> validFunctionList = new LinkedList<>();
        for(Map.Entry<Function,Node> entry : this.validFunctions.entrySet()){
            validFunctionList.add(entry.getKey());
        }

        return validFunctionList;
    }

    /**
     * This method returns all triples used in the query.
     *
     * @return A set of all triples used in the query.
     */
    public Set<TriplePath> getTriplePaths() {
        return triplePaths;
    }

    public Set<Triple> getOptionalTriples(){
        return this.optionalTriples;
    }

    /**
     * This method extracts all variables of the select statement (result variables)
     *
     * @return A list of all result variables
     */
    public List<String> getResultVars(){
        return OpAsQuery.asQuery(this.currentAlgebra).getResultVars();
    }

    public List<String> getSubjectVars(){
        return this.subjectVars;
    }

    public List<String> getOriginalResultVars(){
        return this.originalQuery.getResultVars();
    }

    /**
     * This method is used to retrieve the blocks that needed to be removed from the query.
     *
     * @return A list of all elements that are temporarily removed from the query (e.g. FILTER or MINUS).
     */
    public List<Element> getSubtractionElements() {
        return subtractionElements;
    }

    public boolean isDistinct() {
        return this.distinct;
    }

    public boolean hasLimit() {
        return this.limit > 0;
    }

    public boolean hasOrderBy(){
        return this.orderByConditions != null;
    }

    public boolean hasGroupBy() { return this.groupByConditions != null; }

    public boolean hasFilter(){
        return this.filterExpr != null;
    }

    public int getLimit() {
        return this.limit;
    }

    public ExprList getFilterExpr(){
        return this.filterExpr;
    }

    public List<SortCondition> getOrderByConditions() {
        return orderByConditions;
    }

    public VarExprList getGroupByConditions() {
        return groupByConditions;
    }

    public Map<Var, Expr> getSelectExpressions() {
        return selectExpressions;
    }

    public void setSubtractionAlgebra(Op algebra){
        Query query = QueryFactory.create(OpAsQuery.asQuery(algebra).toString());
        this.subtractionAlgebra = Algebra.compile(query);
    }

    public void setPreconditionAlgebra(Op algebra){
        Query query = QueryFactory.create(OpAsQuery.asQuery(algebra).toString());
        this.preconditionAlgebra = Algebra.compile(query);
    }

    public void setOptionalAlgebra(Op algebra) {
        Query query = QueryFactory.create(OpAsQuery.asQuery(algebra).toString());
        this.optionalAlgebra = Algebra.compile(query);
    }

    public void setCurrentAlgebra(Op algebra) {
        Query query = QueryFactory.create(OpAsQuery.asQuery(algebra).toString());
        this.currentAlgebra = Algebra.compile(query);
    }

    public void setAddedVars(Map<String, Pair<String,String>> addedVarsMap) {
        this.addedVarsMap = addedVarsMap;
    }

    /**
     * This method is used to store the subtraction elements of a tunable query inside the object for later reuse.
     *
     * @param subtractionElements A list of all elements that had temporarily be removed from the query
     *                            (e.g. FILTER and MINUS elements)
     */
    public void setSubtractionElements(List<Element> subtractionElements){
        this.subtractionElements = subtractionElements;
    }

    public void setOptionalTriples(Set<Triple> optionalTriples){
        this.optionalTriples = optionalTriples;
    }

    public void setSubjectVars(List<String> subjectVars){
        this.subjectVars = subjectVars;
    }

    public void setDistinct(boolean distinct){
        this.distinct = distinct;
    }

    public void setLimit(int limit){
        this.limit = limit;
    }

    public void setOrderByConditions(List<SortCondition> orderByConditions) {
        this.orderByConditions = orderByConditions;
    }

    public void setGroupByConditions(VarExprList groupByConditions) {
        this.groupByConditions = groupByConditions;
    }

    public void setSelectExpressions(Map<Var,Expr> selectExpressions) {
        this.selectExpressions = selectExpressions;
    }

    public void setFilterExpr(ExprList filterExpr){
        this.filterExpr = filterExpr;
    }

    /**
     * Uses the QueryWalker class to walk over the query structure and extracts alls triples of a query as a list
     * of triple paths and the structure of a query as a list of query blocks. Each query block represents the
     * path to a leaf in a query.
     */
    private Set<TriplePath> extractTriplePaths(){
        BlockWalker walker = new BlockWalker(this.originalQuery);
        walker.walk();

        return walker.getTriplePaths();
    }

    public Map<String,Pair<String,String>> computeResultVarsMap(){
        List<String> resultVars = getResultVars();
        for(TriplePath tp : this.triplePaths){
            if(tp.getObject().isVariable() && resultVars.contains(tp.getObject().getName())){
                String subject;
                if(tp.getSubject().isVariable()) subject = tp.getSubject().getName();
                else subject = tp.getSubject().toString();

                this.resultVarsMap.put(tp.getObject().getName(),new Pair<>(tp.getPredicate().toString(),subject));
            }
        }

        return this.resultVarsMap;
    }

}
