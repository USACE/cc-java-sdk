package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Action {
    private String name;
    private String description;
    private Map<String,DataSource> parameters;
    @JsonProperty
    public String getName(){
        return name;
    }
    @JsonProperty
    public String getDescription(){
        return description;
    }
    @JsonProperty
    public Map<String,DataSource> getParameters(){
        return parameters;
    }
    public void UpdateActionPaths(){
        for(Map.Entry<String, DataSource> apb : parameters.entrySet()){
            parameters.replace(apb.getKey(),apb.getValue().UpdatePaths());
        }
    }
}
