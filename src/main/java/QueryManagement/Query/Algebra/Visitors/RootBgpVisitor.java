package QueryManagement.Query.Algebra.Visitors;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;

import java.util.HashSet;
import java.util.Set;

public class RootBgpVisitor implements OpVisitor {
    private int root = 0;
    private Set<Triple> rootBgps;

    public RootBgpVisitor() {
        this.rootBgps = new HashSet<>();
    }

    public Set<Triple> getRootBgps() {
        return rootBgps;
    }

    @Override
    public void visit(OpBGP opBGP) {
        if(root < 1) rootBgps.addAll(opBGP.getPattern().getList());
    }

    @Override
    public void visit(OpQuadPattern quadPattern) {

    }

    @Override
    public void visit(OpQuadBlock quadBlock) {

    }

    @Override
    public void visit(OpTriple opTriple) {

    }

    @Override
    public void visit(OpQuad opQuad) {

    }

    @Override
    public void visit(OpPath opPath) {

    }

    @Override
    public void visit(OpTable opTable) {

    }

    @Override
    public void visit(OpNull opNull) {

    }

    @Override
    public void visit(OpProcedure opProc) {

    }

    @Override
    public void visit(OpPropFunc opPropFunc) {

    }

    @Override
    public void visit(OpFilter opFilter) {

    }

    @Override
    public void visit(OpGraph opGraph) {

    }

    @Override
    public void visit(OpService opService) {

    }

    @Override
    public void visit(OpDatasetNames dsNames) {

    }

    @Override
    public void visit(OpLabel opLabel) {

    }

    @Override
    public void visit(OpAssign opAssign) {

    }

    @Override
    public void visit(OpExtend opExtend) {

    }

    @Override
    public void visit(OpJoin opJoin) {
        opJoin.getLeft().visit(this);
        opJoin.getRight().visit(this);
    }

    @Override
    public void visit(OpLeftJoin opLeftJoin) {
        opLeftJoin.getLeft().visit(this);
        opLeftJoin.getRight().visit(this);
    }

    @Override
    public void visit(OpUnion opUnion) {
        root++;
        opUnion.getLeft().visit(this);
        opUnion.getRight().visit(this);
        root--;
    }

    @Override
    public void visit(OpDiff opDiff) {

    }

    @Override
    public void visit(OpMinus opMinus) {

    }

    @Override
    public void visit(OpConditional opCondition) {

    }

    @Override
    public void visit(OpSequence opSequence) {

    }

    @Override
    public void visit(OpDisjunction opDisjunction) {

    }

    @Override
    public void visit(OpList opList) {

    }

    @Override
    public void visit(OpOrder opOrder) {

    }

    @Override
    public void visit(OpProject opProject) {
        opProject.getSubOp().visit(this);
    }

    @Override
    public void visit(OpReduced opReduced) {

    }

    @Override
    public void visit(OpDistinct opDistinct) {

    }

    @Override
    public void visit(OpSlice opSlice) {
        opSlice.getSubOp().visit(this);
    }

    @Override
    public void visit(OpGroup opGroup) {

    }

    @Override
    public void visit(OpTopN opTop) {

    }
}
