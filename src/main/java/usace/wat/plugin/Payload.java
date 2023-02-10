package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Dictionary;

public class Payload {
    @JsonProperty
    private Dictionary<String, Object> attributes;
    @JsonProperty
    private DataStore[] stores;
    @JsonProperty
    private DataSource[] inputs;
    @JsonProperty
    private DataSource[] outputs;
    public Dictionary getAttributes(){
        return attributes;
    }
    public DataStore[] getStores(){
        return stores;
    }
    public DataSource[] getInputs(){
        return inputs;
    }
    public DataSource[] getOutputs(){
        return outputs;
    }
}

