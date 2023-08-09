package QueryManagement.Query.Algebra.Manipulation.Transformers;

import FunctionStore.Function.Function;
import QueryManagement.Query.Algebra.Utils.AlgebraBlock;
import QueryManagement.Query.Algebra.Utils.AlgebraManagement;
import QueryManagement.Query.Algebra.Utils.TripleManagement;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import java.util.*;


public class PreConditionTransformer implements Transform {
    private TunableQuery tunableQuery;
    private TripleManagement tripleManagement;
    private Set<Triple> rootTriples;
    private final List<AlgebraBlock> algebraStructure;
    private AlgebraBlock rootBlock;
    private final List<Map.Entry<Function, Node>> sortedFunctionList;
    private Set<Var> addedVariables;
    private Map<String, Pair<String,String>> variableRelationMap;
    private Map<Op,Set<Triple>> addedBlockRelations;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public PreConditionTransformer(TunableQuery tunableQuery,Op algebra, Set<Triple> rootTriples) {
        this.tunableQuery = tunableQuery;
        this.tripleManagement = new TripleManagement();
        // Adding pre-conditions by using transformer and set the precondition query to the transformed query
        this.algebraStructure = AlgebraManagement.determineAlgebraStructure(algebra);
        this.rootTriples = rootTriples;

        // Create a set of all used variables
        this.addedVariables = new HashSet<>();
        this.variableRelationMap = new HashMap<>();
        this.addedVariables.addAll(((OpProject)tunableQuery.getCurrentAlgebra()).getVars());

        // Determine for each block the used block triples
        this.addedBlockRelations = AlgebraManagement.getBlockTriples(algebraStructure);

        // Identifying root blocks
        this.rootBlock = AlgebraManagement.getRootOp(algebraStructure);

        // Sort the function list in order to have a deterministic order
        sortedFunctionList = new LinkedList<>(tunableQuery.getValidFunctionsMap().entrySet());
        sortedFunctionList.sort(Comparator.comparingInt(function -> function.getKey().getId()));
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public Map<String,Pair<String,String>> getVariableRelationMap() {
        return variableRelationMap;
    }

    @Override
    public Op transform(OpTable opUnit) {
        return opUnit;
    }

    @Override
    public Op transform(OpBGP opBGP) {
        for(AlgebraBlock block : algebraStructure){
            if(block.getLastElement().equals(opBGP)){
                return addLeftJoin(opBGP);
            }
        }

        return opBGP;
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        return OpJoin.create(left,right);
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        return OpLeftJoin.createLeftJoin(left,right,opLeftJoin.getExprs());
    }

    @Override
    public Op transform(OpUnion opUnion, Op left, Op right) {
        return new OpUnion(left,right);
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        List<Var> vars = opProject.getVars();
        vars.forEach(this.addedVariables::remove);
        vars.addAll(this.addedVariables);

        return new OpProject(subOp,vars);
    }

    // Not needed since this blocks will be filtered beforehand

    @Override
    public Op transform(OpTriple opTriple) {
        return null;
    }

    @Override
    public Op transform(OpQuad opQuad) {
        return null;
    }

    @Override
    public Op transform(OpPath opPath) {
        return null;
    }

    @Override
    public Op transform(OpDatasetNames dsNames) {
        return null;
    }

    @Override
    public Op transform(OpQuadPattern quadPattern) {
        return null;
    }

    @Override
    public Op transform(OpQuadBlock quadBlock) {
        return null;
    }

    @Override
    public Op transform(OpNull opNull) {
        return null;
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpGraph opGraph, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpService opService, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpProcedure opProcedure, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpPropFunc opPropFunc, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpLabel opLabel, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpAssign opAssign, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpDiff opDiff, Op left, Op right) {
        return null;
    }

    @Override
    public Op transform(OpMinus opMinus, Op left, Op right) {
        return null;
    }

    @Override
    public Op transform(OpConditional opCondition, Op left, Op right) {
        return null;
    }

    @Override
    public Op transform(OpSequence opSequence, List<Op> elts) {
        return null;
    }

    @Override
    public Op transform(OpDisjunction opDisjunction, List<Op> elts) {
        return null;
    }

    @Override
    public Op transform(OpExt opExt) {
        return null;
    }

    @Override
    public Op transform(OpList opList, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpTopN opTop, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpDistinct opDistinct, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpReduced opReduced, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpSlice opSlice, Op subOp) {
        return null;
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        return null;
    }

    // *****************************************************************************************************************
    // Private Methods
    // *****************************************************************************************************************

    private Op addLeftJoin(OpBGP opBGP){
        Op highestBlockOp = AlgebraManagement.getHighestBlockOp(algebraStructure,opBGP);
        Set<Triple> blockTriples = this.addedBlockRelations.get(highestBlockOp);
        Set<Triple> opBgpTriples = getBgpTriples(opBGP);

        List<BasicPattern> patternList = new LinkedList<>();
        patternList.add(opBGP.getPattern());

        boolean isRoot = this.rootBlock != null && this.rootBlock.getLastElement().equals(opBGP);

        // Test for each function if the corresponding input relation needs to be added to the block
        for(Map.Entry<Function,Node> functionEntry : this.sortedFunctionList) {
            if(isValidTransformation(opBgpTriples,blockTriples,functionEntry)) {
                // Create a Triple that will later be added as optional element to the query
                Triple triple = tripleManagement.createTriple(tunableQuery,functionEntry);
                this.addedVariables.add(Var.alloc(triple.getObject()));

                String subject, predicate;
                if(triple.getSubject().isVariable()) subject = triple.getSubject().getName();
                else subject = triple.getSubject().toString();
                if(triple.getPredicate().isVariable()) predicate = triple.getPredicate().getName();
                else predicate = triple.getPredicate().toString();

                String varName = triple.getObject().getName();
                this.variableRelationMap.put(varName,new Pair<>(predicate,subject));

                // Create Pattern and save it for later (left join interlacing)
                updatePatternList(patternList,triple);

                // In case this is a root block, the added triple needs to be added to the rootTriples.
                if(isRoot) this.rootTriples.add(triple);
                updateBlockRelations(highestBlockOp,blockTriples,triple);
            }
        }

        // Create left join chain (i.e. adding all pattern inside patternList as optionals)
        return AlgebraManagement.createJoinChain(patternList);
    }

    private void updateBlockRelations(Op highestJoin, Set<Triple> blockTriples, Triple triple){
        blockTriples.add(triple);
        this.addedBlockRelations.put(highestJoin,blockTriples);
    }

    private void updatePatternList(List<BasicPattern> patternList, Triple triple){
        List<Triple> tripleList = new LinkedList<>();
        tripleList.add(triple);
        patternList.add(BasicPattern.wrap(tripleList));
    }

    private Set<Triple> getBgpTriples(OpBGP opBGP){
        Set<Triple> addedTriples = new HashSet<>(opBGP.getPattern().getList());

        AlgebraBlock block = AlgebraManagement.getAlgebraBlock(this.algebraStructure,opBGP);
        Op op = Objects.requireNonNull(block).getElement(block.getDepth()-2);
        if(op instanceof OpLeftJoin leftJoin && leftJoin.getRight() instanceof OpBGP){
            addedTriples.addAll(((OpBGP) ((OpLeftJoin) op).getRight()).getPattern().getList());
        }

        return addedTriples;
    }

    private boolean isValidTransformation(Set<Triple> bgpTriples, Set<Triple> blockTriples,
                                          Map.Entry<Function,Node> functionEntry)
    {
        String inputRelation = functionEntry.getKey().getInputRelation();
        boolean containsSubject = TripleManagement.containsSubject(bgpTriples,functionEntry.getValue());
        boolean blockContainsPred = TripleManagement.containsPredicate(blockTriples,inputRelation);
        boolean rootContainsPred = TripleManagement.containsPredicate(rootTriples,inputRelation);

        return  containsSubject && !blockContainsPred && !rootContainsPred;
    }

}
