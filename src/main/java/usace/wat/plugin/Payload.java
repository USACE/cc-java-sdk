package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Payload {
    @JsonProperty
    private Map<String, Object> attributes;
    @JsonProperty
    private DataStore[] stores;
    @JsonProperty
    private DataSource[] inputs;
    @JsonProperty
    private DataSource[] outputs;
    public Map<String,Object> getAttributes(){
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

