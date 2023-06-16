package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Action {
/*    @JsonProperty
    private String name;
    @JsonProperty
    private String description;*/
    public void UpdateActionPaths();
    public void ComputeAction();
}
