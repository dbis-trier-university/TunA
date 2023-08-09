package DataManagement.Integration;

import DataManagement.Clustering.GapExperiment;
import DataManagement.Clustering.TwoGramOverlapDissimilarity;
import DataManagement.DataCrawler.HttpResponse;
import FunctionStore.Function.FuncInstance;
import QueryManagement.Processor.TunableQueryProcessor;
import QueryManagement.Query.Result.GapMap;
import QueryManagement.Query.Result.SolutionMap;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.opencompare.hac.HierarchicalAgglomerativeClusterer;
import org.opencompare.hac.agglomeration.AgglomerationMethod;
import org.opencompare.hac.agglomeration.AverageLinkage;
import org.opencompare.hac.dendrogram.*;
import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VotingBasedIntegrator {
    TunableQueryProcessor tqp;
    List<QuerySolution> localSolutions;
    GapMap gapMap;
    Map<String, Pair<String,String>> varRelationMap;
    List<Pair<FuncInstance, HttpResponse>> responses;
    List<String> handeled;
    public SolutionMap solutionMap;
    public int untrustedCounter = 0;

    public VotingBasedIntegrator(TunableQueryProcessor tqp,
                                 List<QuerySolution> localSolutions,
                                 GapMap gapMap,
                                 Map<String, Pair<String, String>> varRelationMap, // TODO
                                 List<Pair<FuncInstance, HttpResponse>> responses)
    {
        this.tqp = tqp;
        this.localSolutions = localSolutions;
        this.gapMap = gapMap;
        this.varRelationMap = varRelationMap;
        this.responses = responses;
        this.handeled = new LinkedList<>();
        this.solutionMap = new SolutionMap(gapMap,responses);
    }

    public List<List<String>> integrate() {
        List<String> resultVars = tqp.getTQuery().getResultVars();
        Map<String,Pair<String,String>> addedVarsMap = tqp.getTQuery().getAddedVarsMap();
        resultVars.removeIf(item -> addedVarsMap.containsKey(item));

        List<List<String>> integratedResult = new LinkedList<>();
        integratedResult.add(resultVars);

        for (int i = 0; i < localSolutions.size(); i++) {
            QuerySolution solution = localSolutions.get(i);
            List<String> row = handleRow(i,resultVars,solution);
            integratedResult.add(row);
        }

        return integratedResult;
    }

    private List<String> handleRow(int i, List<String> resultVars, QuerySolution solution){
        List<String> row = new LinkedList<>();

        for(String var : resultVars){
            RDFNode rdfNode = solution.get(var);

            if(rdfNode != null) row.add(rdfNode.toString());
            else {
                String relation = varRelationMap.get(var).getLeft();
                row.addAll(handleExternalData(i,relation));
            }
        }

        return row;
    }

    private List<String> handleExternalData(int i, String relation){
        List<String> row = new LinkedList<>();
        Map<FuncInstance,String> extrDataList = solutionMap.get(i,relation);

        if(extrDataList != null && extrDataList.size() > 1) {
            String result = vote(tqp,extrDataList);
            handeled.add(result);
            row.add(result);
        } else if(extrDataList != null) {
            String value = new LinkedList<>(extrDataList.entrySet()).get(0).getValue();
            handeled.add(value);
            row.add(value);
        } else {
            row.add(null);
        }

        return row;
    }

    private String vote(TunableQueryProcessor tqp, Map<FuncInstance,String> result){
        List<String> resultList = new LinkedList<>();
        for(Map.Entry<FuncInstance,String> entry : result.entrySet()) resultList.add(entry.getValue());

        double threshold = (100.0 - (tqp.getSetting().get("Reliability") * 100))/100.0;
        Dendrogram dendrogram = computeDendrogram(resultList);
        DendrogramNode node = cutOffAt(threshold,(MergeNode) dendrogram.getRoot());
        double confidence = node.getObservationCount()/((double) result.size());

//        if(((MergeNode) node).getDissimilarity() <= threshold && confidence >= tqp.getSetting().get("Reliability")) {
        if(confidence > 0.5) {
            return determineIntegrationValue(resultList, node);
        }
        else if(resultList.size() == 2) {
            FuncInstance maxInstance = null;
            for(Map.Entry<FuncInstance,String> entry : result.entrySet()){
                if(maxInstance == null || entry.getKey().getAlignments().get(0).getReliability() > maxInstance.getAlignments().get(0).getReliability())
                    maxInstance = entry.getKey();
            }

            return result.get(maxInstance);
        }
        else {
            this.untrustedCounter++;
            return null;
        }
    }

    private Dendrogram computeDendrogram(List<String> result){
        Experiment experiment = new GapExperiment(result);
        DissimilarityMeasure dissimilarityMeasure = new TwoGramOverlapDissimilarity();
        AgglomerationMethod agglomerationMethod = new AverageLinkage();
        DendrogramBuilder dendrogramBuilder = new DendrogramBuilder(experiment.getNumberOfObservations());
        HierarchicalAgglomerativeClusterer clusterer = new HierarchicalAgglomerativeClusterer(experiment, dissimilarityMeasure, agglomerationMethod);
        clusterer.cluster(dendrogramBuilder);

        return dendrogramBuilder.getDendrogram();
    }

    private DendrogramNode cutOffAt(double threshold, MergeNode node){
        while (node.getDissimilarity() > threshold){
            if(node.getLeft() instanceof MergeNode mergeLeft && node.getRight() instanceof MergeNode mergeRight){
                if(mergeLeft.getDissimilarity() >= mergeRight.getDissimilarity()){
                    node = mergeRight;
                } else {
                    node = mergeLeft;
                }
            } if(node.getLeft() instanceof MergeNode mergeNode){
                node = mergeNode;
            } else if(node.getRight() instanceof MergeNode mergeNode) {
                node = mergeNode;
            } else {
                return node.getLeft();
            }
        }

        return node;
    }

    private String determineIntegrationValue(List<String> result, DendrogramNode node){
        DendrogramNode dNode = node;

        while (dNode.getLeft() != null || dNode.getRight() != null){
            if(dNode.getLeft() instanceof MergeNode mergeLeft && dNode.getRight() instanceof MergeNode mergeRight){
                if(mergeLeft.getDissimilarity() >= mergeRight.getDissimilarity()){
                    dNode = dNode.getRight();
                } else {
                    dNode = dNode.getLeft();
                }
            } if(dNode.getLeft() instanceof MergeNode mergeNode){
                dNode = dNode.getLeft();
            } else if(dNode.getRight() instanceof MergeNode mergeNode) {
                dNode = dNode.getRight();
            } else {
                dNode = dNode.getLeft();
            }
        }

        return result.get(((ObservationNode) dNode).getObservation());
    }
}
