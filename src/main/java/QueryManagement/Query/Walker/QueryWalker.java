package QueryManagement.Query.Walker;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.*;

/**
 * This class can be used to "walk" over a query in a top-down approach. This stands in contrast to the apache jena
 * process which is a bottom-up approach.
 */
public abstract class QueryWalker {
    private Query query;

    public QueryWalker(Query query){
        this.query = query;
    }

    public void walk(){
        walk(this.query.getQueryPattern());
    }

    public void walk(Element element){
        if(element instanceof ElementGroup){
            walkGroup((ElementGroup) element);
        } else if(element instanceof ElementPathBlock){
            walkPathBlock((ElementPathBlock) element);
        } else if(element instanceof ElementUnion){
            walkUnion((ElementUnion) element);
        } else if (element instanceof ElementOptional){
            walkOptional((ElementOptional) element);
        } else if (element instanceof ElementMinus){
            walkMinus((ElementMinus) element);
        }

    }

    // *****************************************************************************************************************
    // Abstract Methods
    // *****************************************************************************************************************

    abstract void walkGroup(ElementGroup group);
    abstract void walkPathBlock(ElementPathBlock epb);
    abstract void walkUnion(ElementUnion union);
    abstract void walkOptional(ElementOptional optional);
    abstract void walkMinus(ElementMinus minus);

}
