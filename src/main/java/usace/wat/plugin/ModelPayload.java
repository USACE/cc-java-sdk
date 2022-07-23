package usace.wat.plugin;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;

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

