package QueryManagement.Query.Algebra.Manipulation.Transformers;

import QueryManagement.Query.Algebra.Utils.AlgebraManagement;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OptionalTransformer implements Transform {
    private final List<String> resultVars;
    private Set<Node> subjectNodes;


    public OptionalTransformer(TunableQuery tunableQuery) {
        this.resultVars = tunableQuery.getResultVars();
        this.subjectNodes = new HashSet<>();
    }

    public List<String> getSubjectNodes(){
        List<String> subjectVars = new LinkedList<>();

        for(Node node : this.subjectNodes){
            if(node.isVariable()) subjectVars.add(node.getName());
            else subjectVars.add(node.toString());
        }

        return subjectVars;
    }

    @Override
    public Op transform(OpTable opUnit) {
        return opUnit;
    }

    @Override
    public Op transform(OpBGP opBGP) {
        for(Triple triple : opBGP.getPattern().getList()){
            this.subjectNodes.add(triple.getSubject());
        }

        return opBGP;
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        return OpJoin.create(left,right);
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        if(left instanceof OpBGP opBGP) return transformToLeftJoinChain(opBGP,right);
        else return OpLeftJoin.create(left,right,opLeftJoin.getExprs());
    }

    @Override
    public Op transform(OpUnion opUnion, Op left, Op right) {
        if(left instanceof OpBGP opBGPLeft) left = transformToLeftJoinChain(opBGPLeft,null);
        if(right instanceof OpBGP opBGPRight) right = transformToLeftJoinChain(opBGPRight,null);

        return OpUnion.create(left,right);
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        if(subOp instanceof OpBGP opBGP) subOp = transformToLeftJoinChain(opBGP,null);

        List<Var> vars = opProject.getVars();
//        for(Node node : this.subjectNodes){
//            if(node.isVariable()) vars.add((Var) node);
//        }

        return new OpProject(subOp,vars);
    }

    // Not needed methods

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

    public Op transformToLeftJoinChain(OpBGP opBGP, Op op){
        Op newLeft;

        List<Triple> optionals = new LinkedList<>();
        for(Triple triple : opBGP.getPattern().getList()){
            if(triple.getObject().isVariable() && this.resultVars.contains(triple.getObject().getName())){
                optionals.add(triple);
            }
        }
        opBGP.getPattern().getList().removeAll(optionals);

        List<BasicPattern> patternList = new LinkedList<>();
        patternList.add(opBGP.getPattern());

        for(Triple triple : optionals){
            BasicPattern tmpPattern = new BasicPattern();
            tmpPattern.add(triple);
            patternList.add(tmpPattern);
        }

        newLeft = AlgebraManagement.createJoinChain(patternList);

        if(op == null) return newLeft;
        else if(newLeft != null) return OpLeftJoin.createLeftJoin(newLeft,op,null);
        else return OpLeftJoin.createLeftJoin(opBGP,op,null);
    }
}
