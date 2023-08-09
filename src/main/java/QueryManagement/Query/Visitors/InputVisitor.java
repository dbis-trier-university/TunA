package QueryManagement.Query.Visitors;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputVisitor implements ElementVisitor {

    private Set<Triple> variableRelations;

    public InputVisitor() {
        this.variableRelations = new HashSet<>();
    }

    public Set<Triple> getVariableRelations() {
        return this.variableRelations;
    }

    @Override
    public void visit(ElementTriplesBlock el) {

    }

    @Override
    public void visit(ElementPathBlock el) {
        List<TriplePath> triplePathList = el.getPattern().getList();
        for(TriplePath path : triplePathList){
            if(!path.getObject().isLiteral() && !path.getPredicate().hasURI(RDF.type.getURI()))
                this.variableRelations.add(new Triple(path.getSubject(),path.getPredicate(),path.getObject()));
        }
    }

    @Override
    public void visit(ElementFilter el) {

    }

    @Override
    public void visit(ElementAssign el) {

    }

    @Override
    public void visit(ElementBind el) {

    }

    @Override
    public void visit(ElementData el) {

    }

    @Override
    public void visit(ElementUnion el) {

    }

    @Override
    public void visit(ElementOptional el) {

    }

    @Override
    public void visit(ElementGroup el) {
        List<Element> elementList = el.getElements();
        for(Element e : elementList){
            e.visit(this);
        }
    }

    @Override
    public void visit(ElementDataset el) {

    }

    @Override
    public void visit(ElementNamedGraph el) {

    }

    @Override
    public void visit(ElementExists el) {

    }

    @Override
    public void visit(ElementNotExists el) {

    }

    @Override
    public void visit(ElementMinus el) {

    }

    @Override
    public void visit(ElementService el) {

    }

    @Override
    public void visit(ElementSubQuery el) {

    }
}
