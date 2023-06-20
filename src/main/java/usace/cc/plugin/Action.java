package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Action {
    @JsonProperty
    public String getName();
    @JsonProperty
    public String getDescription();
    @JsonProperty
    public Map<String,ActionParameterBase> getParameters();
    public void UpdateActionPaths();
    public void ComputeAction();
}
