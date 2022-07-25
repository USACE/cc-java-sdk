package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ResourcedInternalPathData {
    @JsonProperty
    private String pathname;
    @JsonProperty
    private String filename;
    @JsonProperty
    private String internal_path;
    @JsonProperty
    private ResourceInfo resource_info;
    public String getPathName(){
        return pathname;
    }
    public String getFileName(){
        return filename;
    }
    public String getInternalPath(){
        return internal_path;
    }
    public ResourceInfo getResourceInfo(){
        return resource_info;
    }
}