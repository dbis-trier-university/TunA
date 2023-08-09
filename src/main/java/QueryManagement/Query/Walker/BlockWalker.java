package QueryManagement.Query.Walker;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;

import java.util.HashSet;
import java.util.Set;

public class BlockWalker extends QueryWalker {
    private final Set<TriplePath> triplePaths = new HashSet<>();

    public BlockWalker(Query query) {
        super(query);
    }

    public Set<TriplePath> getTriplePaths() {
        return triplePaths;
    }

    void walkGroup(ElementGroup group){
        for (Element element : group.getElements()) {
            walk(element);
        }
    }

    void walkPathBlock(ElementPathBlock block){
        triplePaths.addAll(block.getPattern().getList());
    }

    void walkUnion(ElementUnion union){
        for(Element element : union.getElements()){
            walk(element);
        }
    }

    void walkOptional(ElementOptional optional){
        walk(optional.getOptionalElement());
    }

    void walkMinus(ElementMinus minus){
        walk(minus.getMinusElement());
    }
}
