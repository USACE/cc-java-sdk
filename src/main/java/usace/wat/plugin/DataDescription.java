package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class DataDescription {
    @JsonProperty
    private String name;
    @JsonProperty
    private String parameter;
    @JsonProperty
    private String format;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
