package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Action {
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private Map<String,String> parameters;
    public Map<String,String> getParameters(){
        return parameters;
    }
}
