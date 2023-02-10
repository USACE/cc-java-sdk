package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class DataSource {
    @JsonProperty
    private String name;
    @JsonProperty
    private String id;
    @JsonProperty
    private String store_name;
    @JsonProperty
    private String[] paths;
    public String getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String[] getPaths(){
        return paths;
    }
    public String getStoreName(){
        return store_name;
    }
}