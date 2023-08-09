package Configuration.Service;

import java.util.List;
import java.util.Map;

public class ServiceObject {
    private String label, name, url, format, secret;
    private int timeout;
    private List<Parameter> parameters;

    public ServiceObject(String label, String name, String url, String format, String secret, int timeout,
                         List<Parameter> parameters)
    {
        this.label = label;
        this.name = name;
        this.url = url;
        this.format = format;
        this.secret = secret;
        this.timeout = timeout;
        this.parameters = parameters;
    }

    public String buildSingleParameterCallUrl(String inputValue){
        Parameter p = this.parameters.get(0);

        String callUrl;
        callUrl = this.url.replace("{format}",this.format);
        if(secret != null) callUrl = callUrl.replace("{key}",this.secret);
        callUrl = callUrl.replace("{" + p.getName() + "}",inputValue);

        return callUrl;
    }

    public String buildCallUrl(Map<String,String> inputValues){
        String callUrl;
        callUrl = this.url.replace("{format}",this.format);
        if(secret != null) callUrl = callUrl.replace("{key}",this.secret);

        for (Parameter p : this.parameters) {
            callUrl = callUrl.replace("{" + p.getName() + "}",inputValues.get(p.getName()));
        }

        return callUrl;
    }

    /*******************************************************************************************************************
     * Getter and Setter
     ******************************************************************************************************************/

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public int getTimeout() {
        return timeout;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
}
