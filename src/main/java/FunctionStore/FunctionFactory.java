package FunctionStore;

import FunctionStore.Function.Function;
import FunctionStore.Function.Utils.Alignment;
import FunctionStore.Function.Utils.LocalPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class FunctionFactory {

    /*******************************************************************************************************************
     * Public Methods
     ******************************************************************************************************************/

    public static Function createFunctionFromJson(JSONObject obj){
        List<Alignment> alignments = new LinkedList<>();

        JSONObject meta = obj.getJSONObject("meta");
        String database = meta.getString("database");
        String api = meta.getString("api");
        String inputType = meta.getString("inputType");
        String inputRelation = meta.getString("inputRelation");
        int responseTime = meta.getInt("responseTime");
        double responseProbability = meta.getDouble("responseProbability");

        JSONArray alignmentArray = obj.getJSONArray("alignments");
        for (int i = 0; i < alignmentArray.length(); i++) {
            JSONObject jsonObject = alignmentArray.getJSONObject(i);

            double reliability = jsonObject.getDouble("reliability");
            List<LocalPath> localRelations = createLocalRelations(jsonObject);
            List<String> remoteRelations = createRemoteRelations(jsonObject);

            Alignment alignment = new Alignment(localRelations,remoteRelations,reliability);
            alignments.add(alignment);
        }

        Function function = new Function(database,api,inputType,inputRelation, alignments,
                responseTime,responseProbability);

        return function;
    }

    /*******************************************************************************************************************
     * Private Methods
     ******************************************************************************************************************/

    private static List<LocalPath> createLocalRelations(JSONObject alignment){
        List<LocalPath> localRelations = new LinkedList<>();

        JSONArray localRelationArray = alignment.getJSONArray("relation_path");
        for (int j = 0; j < localRelationArray.length(); j++) {
            List<String> pathList = new LinkedList<>();

            JSONArray pathArray = localRelationArray.getJSONObject(j).getJSONArray("path");
            for (int k = 0; k < pathArray.length(); k++) {
                pathList.add(pathArray.getString(k));
            }

            localRelations.add(new LocalPath(pathList));
        }

        return  localRelations;
    }

    private static List<String> createRemoteRelations(JSONObject alignment){
        List<String> remoteRelations = new LinkedList<>();

        JSONArray remoteRelationArray = alignment.getJSONArray("api_path");
        for (int j = 0; j < remoteRelationArray.length(); j++) {
            remoteRelations.add(remoteRelationArray.getString(j));
        }

        return  remoteRelations;
    }
}
