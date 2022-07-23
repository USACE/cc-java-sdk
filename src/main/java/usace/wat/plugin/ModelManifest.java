package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ModelManifest {
    @JsonProperty
    private String manifest_id;
    @JsonProperty
    private Plugin plugin;
    @JsonProperty
    private ModelIdentifier model_identifier;
    @JsonProperty
    private FileData[] inputs;
    @JsonProperty
    private FileData[] outputs;
    public String getId(){
        return manifest_id;
    }
    public Plugin getPlugin(){
        return plugin;
    }
    public FileData[] getInputs(){
        return inputs;
    }
    public FileData[] getOutputs(){
        return outputs;
    }
    public ModelIdentifier getModelIdentifier(){
        return model_identifier;
    }
}