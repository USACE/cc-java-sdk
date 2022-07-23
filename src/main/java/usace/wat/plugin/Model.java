package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Model {
    @JsonProperty
    private String name;
    @JsonProperty
    private String alternative;
    public String getName(){
        return name;
    }
    public String getAlternative(){
        return alternative;
    }
}
