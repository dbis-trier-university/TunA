package QueryManagement.Query.Algebra.Utils;

import QueryManagement.Query.Algebra.Visitors.BlockTripleVisitor;
import QueryManagement.Query.Algebra.Visitors.RootBgpVisitor;
import QueryManagement.Query.Algebra.Walker.OpBlockWalker;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;

import java.util.*;

public class AlgebraManagement {

    public static Map<Op,Set<Triple>> getBlockTriples(List<AlgebraBlock> algebraStructure){
        // Key is the root of a block (e.g. join, union)
        // and value is a set of all used triples inside the block
        Map<Op,Set<Triple>> blockTriples = new HashMap<>();

        // Collect all triples that are used in a block
        for(AlgebraBlock block : algebraStructure){
            Op blockRoot = getHighestBlockOp(block);

            BlockTripleVisitor visitor = new BlockTripleVisitor();
            if(blockRoot != null) blockRoot.visit(visitor);
            Set<Triple> blockTriplesList = new HashSet<>(visitor.getBlockTriples());

            blockTriples.put(blockRoot,blockTriplesList);
        }

        return blockTriples;
    }

    public static Op getHighestBlockOp(List<AlgebraBlock> algebraStructure, OpBGP opBGP){
        AlgebraBlock block = getAlgebraBlock(algebraStructure,opBGP);

        return getHighestBlockOp(block);
    }

    public static Op getHighestBlockOp(AlgebraBlock block){
        Op highestBlockOp = null;

        for (int i = block.getDepth()-1; i >= 0; i--) {
            Op op = block.getElement(i);
            if(op instanceof OpLeftJoin || op instanceof OpJoin) highestBlockOp = op;
            else if(op instanceof OpUnion && highestBlockOp == null) highestBlockOp = block.getElement(i+1);
        }

        if(highestBlockOp == null){
            highestBlockOp = block.getElement(0);
        }

        return highestBlockOp;
    }

    public static List<AlgebraBlock> determineAlgebraStructure(Op algebra){
        OpBlockWalker walker = new OpBlockWalker(algebra);
        walker.walk();
        return walker.getAlgebraStructure();
    }

    public static AlgebraBlock getRootOp(List<AlgebraBlock> algebraStructure){
        List<AlgebraBlock> rootBlocks = new LinkedList<>();

        for(AlgebraBlock block : algebraStructure){
            if(!block.containsUnion()){
                rootBlocks.add(block);
            }
        }

        if(rootBlocks.size() > 0){
            return rootBlocks.get(0);
        } else {
            return null;
        }
    }

    public static Op createJoinChain(List<BasicPattern> patternList){
        Op joinChain = new OpBGP(patternList.get(0));

        if(((OpBGP) joinChain).getPattern().isEmpty()){
            joinChain = OpTable.unit();
        }

        for (int i = 1; i < patternList.size(); i++) {
            OpBGP right = new OpBGP(patternList.get(i));
            joinChain = OpLeftJoin.createLeftJoin(joinChain,right,null);
        }

        return joinChain;
    }

    public static RootBgpVisitor determineRootTriples(Op algebra){
        RootBgpVisitor visitor = new RootBgpVisitor();
        algebra.visit(visitor);

        return visitor;
    }

    public static AlgebraBlock getAlgebraBlock(List<AlgebraBlock> algebraStructure, OpBGP opBGP){
        for(AlgebraBlock block : algebraStructure){
            if(block.getLastElement() == opBGP)
                return block;
        }

        return null;
    }

    public static boolean isOnlyUnionQuery(List<AlgebraBlock> algebraStructure){
        for(AlgebraBlock block : algebraStructure){
            if(!block.containsUnion()) return false;
        }

        return true;
    }
}
