package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ModelConfiguration {
    @JsonProperty
    private String model_name;
    @JsonProperty
    private String model_alternative;
    public String ModelName(){
        return model_name;
    }
    public String ModelAlternative(){
        return model_alternative;
    }
}
