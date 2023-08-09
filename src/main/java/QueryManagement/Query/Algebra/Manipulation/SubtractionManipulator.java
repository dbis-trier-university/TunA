package QueryManagement.Query.Algebra.Manipulation;

import QueryManagement.Query.Algebra.Manipulation.Transformers.SubtractionTransformer;
import QueryManagement.Query.TunableQuery;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;

import java.util.Map;

public class SubtractionManipulator {
    public static void removeRefinements(TunableQuery tunableQuery){
        Query currentQuery = OpAsQuery.asQuery(tunableQuery.getCurrentAlgebra());
        tunableQuery.setDistinct(currentQuery.isDistinct());
        if(currentQuery.hasLimit()) tunableQuery.setLimit((int) currentQuery.getLimit());

        if(currentQuery.hasGroupBy()){
            tunableQuery.setGroupByConditions(currentQuery.getGroupBy());

            // Extract aggregate expressions from the select statement and replace them with the aggregate variable
            extractAggregateExprs(tunableQuery,currentQuery.getProject());
        }

        if(currentQuery.hasOrderBy()){
            tunableQuery.setOrderByConditions(currentQuery.getOrderBy());
        }

        transform(tunableQuery);
    }

    private static void transform(TunableQuery tunableQuery){
        SubtractionTransformer trans = new SubtractionTransformer(tunableQuery.getSelectExpressions());
        Op subtractionAlgebra = Transformer.transform(trans,tunableQuery.getCurrentAlgebra());

        tunableQuery.setFilterExpr(trans.getFilterList());
        tunableQuery.setSubtractionAlgebra(subtractionAlgebra);
        tunableQuery.setCurrentAlgebra(subtractionAlgebra);
        tunableQuery.setOptionalTriples(trans.getOptionalTriples());
    }

    /**
     * Extracts aggregate expressions from the select statement and replaces them with the aggregate variable.
     * Example: COUNT(?title AS ?no) will be replaced by ?title
     *
     * @param tunableQuery Contains all information and transformation steps of a query
     */
    private static void extractAggregateExprs(TunableQuery tunableQuery, VarExprList selectVars){
        Map<Var, Expr> exprs = selectVars.getExprs();
        tunableQuery.setSelectExpressions(exprs);
    }
}
