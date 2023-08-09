package QueryManagement.Query.Algebra.Walker;

import QueryManagement.Query.Algebra.Utils.AlgebraBlock;
import QueryManagement.Query.Algebra.Utils.AlgebraManagement;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;

import java.util.*;

public class OpBlockWalker extends OpWalker {
    private AlgebraBlock block;
    private List<AlgebraBlock> algebraStructure;
    private boolean postProcessed = false;

    public OpBlockWalker(Op algebra) {
        super(algebra);
        block = new AlgebraBlock();
        algebraStructure = new LinkedList<>();
    }

    public List<AlgebraBlock> getAlgebraStructure() {
        if(!postProcessed) doPostProcessing();
        return this.algebraStructure;
    }

    @Override
    public void walkOpUnion(OpUnion opUnion) {
        block.addElement(opUnion);

        walk(opUnion.getLeft());
        walk(opUnion.getRight());

        reset();
    }

    @Override
    public void walkOpJoin(OpJoin opJoin) {
        block.addElement(opJoin);

        walk(opJoin.getLeft());
        walk(opJoin.getRight());

        reset();
    }

    @Override
    public void walkOpLeftJoin(OpLeftJoin opLeftJoin) {
        block.addElement(opLeftJoin);

        walk(opLeftJoin.getLeft());
        walk(opLeftJoin.getRight());

        reset();
    }

    @Override
    public void walkOpBGP(OpBGP opBGP) {
        if(isValid(opBGP) && isMostLeft(opBGP)){
            block.addElement(opBGP);
            algebraStructure.add(block);
            reset();
        }
    }

    @Override
    public void walkOpProject(OpProject opProject) {
        block.addElement(opProject);
        walk(opProject.getSubOp());
    }

    private void reset(){
        AlgebraBlock tmp = new AlgebraBlock();

        for (int i = 0; i < block.getDepth() - 1; i++) {
            tmp.addElement(block.getElement(i));
        }

        block = tmp;
    }

    private void doPostProcessing(){
        // Identify most left block that is not inside a union. This block will be the root block.
        AlgebraBlock mostLeftBlock = null;
        if(!AlgebraManagement.isOnlyUnionQuery(this.algebraStructure)){
            for(AlgebraBlock block : this.algebraStructure){
                if(mostLeftBlock == null || !block.containsUnion() && block.getDepth() > mostLeftBlock.getDepth())
                    mostLeftBlock = block;
            }
        }

        // Remove all other non-union blocks, since they are no root blocks.
        AlgebraBlock finalMostLeftBlock = mostLeftBlock;
        if(AlgebraManagement.isOnlyUnionQuery(algebraStructure))
            this.algebraStructure.removeIf(block -> !block.containsUnion() && !block.equals(finalMostLeftBlock));

        // Ordering list according to block length
        this.algebraStructure = this.algebraStructure.stream().
                sorted(Comparator.comparingInt(AlgebraBlock::getDepth)).toList();

        postProcessed = true;
    }

    // TODO optimize
    private boolean isValid(OpBGP opBGP){
        if(block.getDepth() == 1
                || block.getElement(block.getDepth() - 1) instanceof OpJoin
                || block.getElement(block.getDepth() - 1) instanceof OpUnion)
        {
            return true;
        } else if(block.getElement(block.getDepth() - 1) instanceof OpLeftJoin join
                    && (join.getLeft().equals(opBGP) || isRootOfLeftJoinChain(opBGP) )) // TODO not yet implemented
        {
            return true;
        }

        return false;
    }

    // TODO in case we want to allow queries with only left joins (i.g. each statement is optional)
    private boolean isRootOfLeftJoinChain(OpBGP opBGP){
        return false;
    }

    private boolean isMostLeft(OpBGP opBGP){
        Op op = this.block.getElement(block.getDepth()-1);

        if(op instanceof OpTable opTable && opTable.getTable().isEmpty()){
            return false;
        } else if(op instanceof OpJoin join
                && join.getLeft() instanceof OpLeftJoin leftJoin
                && leftJoin.getLeft() instanceof OpBGP tmpBGP)
        {
            return !test(opBGP,tmpBGP);
        }

        return true;
    }

    private boolean test(OpBGP opBGP, OpBGP tmpBGP){
        Set<Node> opBGPVars = new HashSet<>();
        for(Triple t : opBGP.getPattern().getList()){
            opBGPVars.add(t.getSubject());
        }

        Set<Node> tmpBGPVars = new HashSet<>();
        for(Triple t : tmpBGP.getPattern().getList()){
            tmpBGPVars.add(t.getSubject());
        }

        return tmpBGPVars.containsAll(opBGPVars);
    }

}
