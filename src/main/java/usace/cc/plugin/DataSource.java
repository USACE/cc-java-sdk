package usace.cc.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class DataSource {
    @JsonProperty
    private String Name;
    @JsonProperty
    private String ID;
    @JsonProperty
    private String StoreName;
    @JsonProperty
    private String[] Paths;
    public String getId(){
        return ID;
    }
    public String getName(){
        return Name;
    }
    public String[] getPaths(){
        return Paths;
    }
    public String getStoreName(){
        return StoreName;
    }
}