package QueryManagement.Query.Algebra.Manipulation;

import QueryManagement.Query.Algebra.Manipulation.Transformers.OptionalTransformer;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;

/**
 * This class is used to transform triples inside a query into optional triples.
 */
public class OptionalManipulator {

    public static void transformSelectTriples(TunableQuery tunableQuery){
        OptionalTransformer transformer = new OptionalTransformer(tunableQuery);
        Op transformedAlgebra = Transformer.transform(transformer, tunableQuery.getCurrentAlgebra());

        // Save next transformation step
        tunableQuery.setSubjectVars(transformer.getSubjectNodes());
        tunableQuery.setOptionalAlgebra(transformedAlgebra);
        tunableQuery.setCurrentAlgebra(transformedAlgebra);
    }
}
