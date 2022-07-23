package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class InternalPathData {
    @JsonProperty
    private String id;
    @JsonProperty
    private String pathname;
    public String getPathName(){
        return pathname;
    }
    public String getId(){
        return id;
    }
}