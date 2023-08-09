package DataManagement.DataCrawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.json.XML;

import java.io.IOException;
import java.util.HashMap;

/**
 * <p>The HttpResponse class is used to store (deserialize) Web API responses as objects. It also provides functions
 * to read the content or the application type. Furthermore, there are methods to flatten the response tree into a
 * flat map structure</p>
 */
public class HttpResponse {
    private String content, applicationType;

    HttpResponse(String applicationType, String content){
        this.content = content;
        this.applicationType = applicationType;
    }

    /**
     * <p>Returns the content of an HTTP response (e.g. an JSON response) formatted as String</p>
     *
     * @return Content of an HTTP response formatted as String
     */
    public String getContent() {
        return content;
    }

    /**
     * <p>Returns the application type of an HTTP response.</p>
     *
     * @return Application Type of the HTTP response formatted as String
     */
    public String getApplicationType() {
        return applicationType;
    }

    /**
     * <p>This method is used to convert an HTTP response in form of a json tree into a flatted json
     * (dictionary/map) file. The path to the value is afterwards a predicate string and the value itself
     * is an object (spoken in RDF notation)</p>
     *
     * @param response Is an HTTP response with a content and an application type
     * @return Flattened json file in form of an HashMap
     */
    public static HashMap<String,Object> convertResponse(HttpResponse response){
        try{
            HashMap<String,Object> result = new HashMap<>();
            String jsonString;

            // Check the response type (XML, JSON and other)
            // All formats will be transformed into an flattened JSON version
            if(response.getApplicationType().contains("xml")){
                jsonString = XML.toJSONObject(response.getContent()).toString();
                jsonString = JsonFlattener.flatten(jsonString);
            } else if(response.getApplicationType().contains("json")){
                jsonString = JsonFlattener.flatten(response.getContent());

            } else {
                // Using an external library to flatten JSON (link: https://github.com/wnameless/json-flattener)
                jsonString = JsonFlattener.flatten(response.getContent());
            }

            try {
                //noinspection unchecked
                result = new ObjectMapper().readValue(jsonString, HashMap.class);
            } catch (IOException e) {
                System.out.println("[ResponseConverter.convertResponse]: " + e.getMessage());

                return result;
            }

            return result;
        } catch (Exception e){
            return null;
        }
    }
}
