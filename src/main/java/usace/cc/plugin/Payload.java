package usace.cc.plugin;
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
    @JsonProperty
    private Action[] actions;
    public Map<String,Object> getAttributes(){
        return attributes;
    }
    public DataStore[] getStores(){
        return stores;
    }
    public void setStore(int index, DataStore store){
        stores[index] = store;
    }
    public DataSource[] getInputs(){
        return inputs;
    }
    public DataSource[] getOutputs(){
        return outputs;
    }
    public Action[] getActions(){
        return actions;
    }
}

