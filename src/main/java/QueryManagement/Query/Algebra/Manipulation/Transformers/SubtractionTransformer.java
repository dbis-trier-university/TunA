package QueryManagement.Query.Algebra.Manipulation.Transformers;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubtractionTransformer implements Transform {
    Set<Triple> optionalTriples;
    Map<Var, Expr> exprMap;
    ExprList filterList;

    public SubtractionTransformer(Map<Var, Expr> exprMap){
        this.optionalTriples = new HashSet<>();
        this.exprMap = exprMap;
    }

    public Set<Triple> getOptionalTriples(){
        return this.optionalTriples;
    }

    public ExprList getFilterList(){
        return this.filterList;
    }

    @Override
    public Op transform(OpTable opUnit) {
        return opUnit;
    }

    @Override
    public Op transform(OpBGP opBGP) {
        return opBGP;
    }

    @Override
    public Op transform(OpTriple opTriple) {
        return opTriple;
    }

    @Override
    public Op transform(OpQuad opQuad) {
        return opQuad;
    }

    @Override
    public Op transform(OpPath opPath) {
        return opPath;
    }

    @Override
    public Op transform(OpDatasetNames dsNames) {
        return dsNames;
    }

    @Override
    public Op transform(OpQuadPattern quadPattern) {
        return quadPattern;
    }

    @Override
    public Op transform(OpQuadBlock quadBlock) {
        return quadBlock;
    }

    @Override
    public Op transform(OpNull opNull) {
        return opNull;
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        this.filterList = opFilter.getExprs();
        return subOp;
    }

    @Override
    public Op transform(OpGraph opGraph, Op subOp) {
        return opGraph;
    }

    @Override
    public Op transform(OpService opService, Op subOp) {
        return opService;
    }

    @Override
    public Op transform(OpProcedure opProcedure, Op subOp) {
        return opProcedure;
    }

    @Override
    public Op transform(OpPropFunc opPropFunc, Op subOp) {
        return opPropFunc;
    }

    @Override
    public Op transform(OpLabel opLabel, Op subOp) {
        return opLabel;
    }

    @Override
    public Op transform(OpAssign opAssign, Op subOp) {
        return opAssign;
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        return subOp;
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        return OpJoin.create(left,right);
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        if(right instanceof OpBGP bgp) this.optionalTriples.addAll(bgp.getPattern().getList());

        return opLeftJoin;
    }

    @Override
    public Op transform(OpDiff opDiff, Op left, Op right) {
        return opDiff;
    }

    @Override
    public Op transform(OpMinus opMinus, Op left, Op right) {
        return left;
    }

    @Override
    public Op transform(OpUnion opUnion, Op left, Op right) {
        return opUnion;
    }

    @Override
    public Op transform(OpConditional opCondition, Op left, Op right) {
        return opCondition;
    }

    @Override
    public Op transform(OpSequence opSequence, List<Op> elts) {
        return opSequence;
    }

    @Override
    public Op transform(OpDisjunction opDisjunction, List<Op> elts) {
        return opDisjunction;
    }

    @Override
    public Op transform(OpExt opExt) {
        return opExt;
    }

    @Override
    public Op transform(OpList opList, Op subOp) {
        return opList;
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        return subOp;
    }

    @Override
    public Op transform(OpTopN opTop, Op subOp) {
        return opTop;
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        List<Var> vars = opProject.getVars();

        if(this.exprMap != null){
            vars.removeIf(this.exprMap::containsKey);

            for(Map.Entry<Var,Expr> entry : this.exprMap.entrySet()){
                ExprAggregator expr = (ExprAggregator) entry.getValue();
                Set<Var> set = expr.getAggregator().getExprList().getVarsMentioned();
                vars.addAll(set);
            }
        }

        return new OpProject(subOp,vars);
    }

    @Override
    public Op transform(OpDistinct opDistinct, Op subOp) {
        return subOp;
    }

    @Override
    public Op transform(OpReduced opReduced, Op subOp) {
        return opReduced;
    }

    @Override
    public Op transform(OpSlice opSlice, Op subOp) {
        return subOp;
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        return subOp;
    }
}

