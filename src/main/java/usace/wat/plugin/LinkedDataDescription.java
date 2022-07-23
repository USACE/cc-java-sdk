package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class LinkedDataDescription {
    @JsonProperty
    private String name;
    @JsonProperty
    private String parameter;
    @JsonProperty
    private String format;
    @JsonProperty
    private ResourceInfo resource_info;
    public ResourceInfo getResourceInfo(){
        return resource_info;
    }
    public String getName(){
        return name;
    }
}
