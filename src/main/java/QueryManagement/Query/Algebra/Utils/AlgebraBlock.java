package QueryManagement.Query.Algebra.Utils;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpUnion;

import java.util.LinkedList;
import java.util.List;

public class AlgebraBlock {
    private List<Op> elements;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public AlgebraBlock() {
        this.elements = new LinkedList<>();
    }

    public AlgebraBlock(List<Op> elements) {
        this.elements = elements;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public void addElement(Op op){
        this.elements.add(op);
    }

    public List<Op> getElements() {
        return this.elements;
    }

    public int getDepth(){
        return this.elements.size();
    }

    public Op getElement(int i){
        return this.elements.get(i);
    }

    public OpBGP getLastElement(){
        return ((OpBGP) this.elements.get(this.elements.size()-1));
    }

    public boolean containsUnion(){
        for(Op op : this.elements){
            if(op instanceof OpUnion) return true;
        }

        return false;
    }

    public boolean containsJoin(){
        for(Op op : this.elements){
            if(op instanceof OpLeftJoin) return true;
        }

        return false;
    }
}
