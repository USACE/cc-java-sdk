package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ModelLinks {
    @JsonProperty
    private LinkedDataDescription[] linked_inputs;
    @JsonProperty
    private LinkedDataDescription[] required_outputs;
    public LinkedDataDescription[] getLinked_inputs() {
        return linked_inputs;
    }
    public LinkedDataDescription[] getRequired_outputs() {
        return required_outputs;
    }
}
