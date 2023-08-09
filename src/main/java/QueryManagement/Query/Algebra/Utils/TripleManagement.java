package QueryManagement.Query.Algebra.Utils;

import FunctionStore.Function.Function;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TripleManagement {
    private int index;

    public TripleManagement() {
        this.index = 0;
    }

    public Triple createTriple(TunableQuery tunableQuery, Map.Entry<Function,Node> functionEntry){
        Node s = functionEntry.getValue();
        Node p = NodeFactory.createURI(functionEntry.getKey().getInputRelation());
        Node o = createValidObjectVariable(tunableQuery, s, p);

        return new Triple(s, p, o);
    }

    public Node createValidObjectVariable(TunableQuery tunableQuery, Node s, Node p){
        Set<TriplePath> addedTriples = new HashSet<>();
        Node o = null;

        for(TriplePath tp : tunableQuery.getTriplePaths()){
            if(tp.getSubject().equals(s) && tp.getPredicate().equals(p)){
                o = tp.getObject();
                break;
            }
        }

        if(o == null){
            o = NodeFactory.createVariable("input" + (index++));
            addedTriples.add(new TriplePath(new Triple(s,p,o)));
        }

        tunableQuery.getTriplePaths().addAll(addedTriples);

        return o;
    }

    public static boolean containsPredicate(Set<Triple> rootTriples, String inputRelation){
        for(Triple triple : rootTriples){
            if(triple.getPredicate().isURI() && triple.getPredicate().toString().equals(inputRelation)){
                return true;
            }
        }

        return false;
    }

    public static boolean containsSubject(Set<Triple> rootTriples, Node subject){
        for(Triple triple : rootTriples){
            if(triple.getSubject().equals(subject))
            {
                return true;
            }
        }

        return false;
    }
}
