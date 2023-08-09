package QueryManagement.Query.Algebra.Manipulation;

import QueryManagement.Query.Algebra.Manipulation.Transformers.PreConditionTransformer;
import QueryManagement.Query.Algebra.Utils.AlgebraManagement;
import QueryManagement.Query.Algebra.Visitors.RootBgpVisitor;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;

import java.util.Set;

public class PreConditionManipulator {

    public static void addPreConditions(TunableQuery tunableQuery){
        // Determine root triples (if a root is available)
        Set<Triple> rootTriples = identifyRootTriples(tunableQuery);

        // Transforming algebra by adding pre-conditions (i.e. a chain of left joins)
        Op transformedAlgebra = execTransformer(tunableQuery, rootTriples);

        // Adding the new transformation steps as debug information to the tunable query object
        cleanUp(tunableQuery,transformedAlgebra);
    }

    public static Set<Triple> identifyRootTriples(TunableQuery tunableQuery){
        RootBgpVisitor visitor = AlgebraManagement.determineRootTriples(tunableQuery.getCurrentAlgebra());
        return visitor.getRootBgps();
    }

    public static void cleanUp(TunableQuery tunableQuery, Op transformedAlgebra){
        tunableQuery.setPreconditionAlgebra(transformedAlgebra);
        tunableQuery.setCurrentAlgebra(transformedAlgebra);
    }

    public static Op execTransformer(TunableQuery tunableQuery, Set<Triple> rootTriples) {
        Op algebra = tunableQuery.getCurrentAlgebra();

        // Transform the query by adding pre-conditions
        PreConditionTransformer transformer = new PreConditionTransformer(tunableQuery,algebra,rootTriples);
        Op newAlgebra = Transformer.transform(transformer,algebra);
        tunableQuery.setAddedVars(transformer.getVariableRelationMap());

        return newAlgebra;
    }

}
