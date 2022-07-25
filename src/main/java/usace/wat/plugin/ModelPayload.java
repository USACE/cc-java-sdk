package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelPayload {
    @JsonProperty
    private String payload_id;
    @JsonProperty
    private Model model;
    @JsonProperty
    private int event_index;
    @JsonProperty
    private ResourcedFileData[] inputs;
    @JsonProperty
    private ResourcedFileData[] outputs;
    public String getId(){
        return payload_id;
    }
    public int getEventIndex(){
        return event_index;
    }
    public ResourcedFileData[] getInputs(){
        return inputs;
    }
    public ResourcedFileData[] getOutputs(){
        return outputs;
    }
    public Model getModel(){
        return model;
    }
}

