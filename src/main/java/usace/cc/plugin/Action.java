package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Action {
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private Map<String,ActionParameter> parameters;
    public Map<String,ActionParameter> getParameters(){
        return parameters;
    }
}
