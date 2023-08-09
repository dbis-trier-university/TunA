package FunctionStore;

import Configuration.Configuration;
import FunctionStore.Function.Function;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;
import QueryManagement.Query.Visitors.InputVisitor;
import Utils.Utils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class FunctionStore {
    private static final Logger logger = Configuration.getInstance().getLogger();
    private static FunctionStore functionStore;
    private final List<Function> functions;

    /*******************************************************************************************************************
     * Constructor
     ******************************************************************************************************************/

    private FunctionStore(String path) {
        logger.info("Loading function store");
        this.functions = new LinkedList<>();

        List<String> fileNames = Utils.getFileNames(path);
        for(String name : fileNames){
            try {
                String content = Files.readString(Path.of(path + name));
                functions.add(FunctionFactory.createFunctionFromJson(new JSONObject(content)));
            } catch (IOException e) {
                logger.severe("Error while loading function store (IOException): " + e.getMessage());
            }
        }
    }

    /*******************************************************************************************************************
     * Public Methods
     ******************************************************************************************************************/

    public static FunctionStore getInstance(String path){
        if(functionStore == null) functionStore = new FunctionStore(path);

        return functionStore;
    }

    public static FunctionStore getInstance(){
        return getInstance(Configuration.getInstance().getFunctionStorePath());
    }

    public static Function get(String name){
        for(Function function : functionStore.functions){
            if(function.getApiName().equals(name)) return function;
        }

        throw new NoSuchElementException("The API with the name " + name + " could not be found.");
    }

    public Map<Function, Node> getAllMatchingFunctions(Set<Triple> variableRelations){
        Map<Function,Node> functions = new HashMap<>();

        for(Triple mapping : variableRelations){
            String predicate = mapping.getPredicate().toString();
            HashMap<Function,Node> tmp = getMatchingFunctions(predicate,mapping.getSubject(),variableRelations);
            functions.putAll(tmp);
        }

        return  functions;
    }

    public Map<Function,Node> getValidFunctionsMap(Query query){
        InputVisitor iv = new InputVisitor();
        Element where = query.getQueryPattern();
        ElementWalker.walk(where,iv);
        Set<Triple> variableRelations = iv.getVariableRelations();

        return functionStore.getAllMatchingFunctions(variableRelations);
    }

    /*******************************************************************************************************************
     * Private Methods
     ******************************************************************************************************************/

    private HashMap<Function,Node> getMatchingFunctions(String predicate, Node variable, Set<Triple> variableRelations){
        HashMap<Function,Node> matchingFunctions = new HashMap<>();

        for(Function function : this.functions){
            for(Alignment alignment : function.getAlignments()){
                for (LocalPath localRelationPath : alignment.getLocalRelations()) {
                    if (isMatchingFunction(predicate, localRelationPath)) {

                        // In case of a simple 1:1 or 1:n mapping
                        if (localRelationPath.size() == 1) {
                            matchingFunctions.put(function, variable);
                        } else {
                            Node node = traverseTroughQuery(variable,localRelationPath.get(0),localRelationPath,variableRelations);
                            if(node != null) matchingFunctions.put(function,node);
                        }
                    }
                }
            }
        }

        return matchingFunctions;
    }


    private boolean isMatchingFunction(String localRelation, LocalPath localPath) {
        return localPath.get(localPath.size() - 1).equals(localRelation);
    }

    private Node traverseTroughQuery(Node variable, String predicate,
                                     LocalPath localRelationPath, Set<Triple> variableRelations)
    {
        for(Triple t : variableRelations){
            if(t.getObject().equals(variable) && t.getPredicate().hasURI(predicate)){
                return t.getSubject();
            } else if(t.getObject().equals(variable) && localRelationPath.contains(t.getPredicate().toString())){
                return traverseTroughQuery(t.getSubject(),predicate,localRelationPath,variableRelations);
            }
        }

        return null;
    }
}
