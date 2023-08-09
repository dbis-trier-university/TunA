package QueryManagement.Query.Algebra.Walker;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;

public abstract class OpWalker {
    Op algebra;

    public OpWalker(Op algebra) {
        this.algebra = algebra;
    }

    public void walk(){
        walk(algebra);
    }

    public void walk(Op op){
        if(op instanceof OpJoin){
            walkOpJoin((OpJoin) op);
        } else if(op instanceof OpLeftJoin){
            walkOpLeftJoin((OpLeftJoin) op);
        } else if(op instanceof OpBGP){
            walkOpBGP((OpBGP) op);
        } else if (op instanceof OpProject){
            walkOpProject((OpProject) op);
        } else if (op instanceof OpUnion) {
            walkOpUnion((OpUnion) op);
        }
    }

    // *****************************************************************************************************************
    // Abstract Methods
    // *****************************************************************************************************************

    public abstract void walkOpUnion(OpUnion opUnion);

    public abstract void walkOpJoin(OpJoin opJoin);

    public abstract void walkOpLeftJoin(OpLeftJoin opLeftJoin);

    public abstract void walkOpBGP(OpBGP opBGP);

    public abstract void walkOpProject(OpProject opProject);
}
