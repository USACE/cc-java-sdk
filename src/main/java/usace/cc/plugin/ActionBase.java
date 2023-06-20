package usace.cc.plugin;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public abstract class ActionBase extends StdDeserializer<Action> implements Action  {
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private Map<String,ActionParameterBase> parameters;
    @Override
    public String getName(){
        return name;
    }
    @Override
    public String getDescription(){
        return description;
    }
    @Override
    public Map<String, ActionParameterBase> getParameters(){
        return parameters;
    }
    @Override
    public void UpdateActionPaths(){
        for(Map.Entry<String, ActionParameterBase> apb : parameters.entrySet()){
            parameters.replace(apb.getKey(),apb.getValue().UpdatePaths());
        }
    }
    protected ActionBase(Class<?> vc) {
        super(vc);
    }

}
