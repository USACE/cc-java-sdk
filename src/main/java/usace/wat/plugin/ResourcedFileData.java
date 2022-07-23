package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ResourcedFileData {
    @JsonProperty
    private String id;
    @JsonProperty
    private String filename;
    @JsonProperty
    private ResourceInfo resource_info;
    @JsonProperty
    private []ResourcedInternalPathData internal_paths;
    public String getId(){
        return id;
    }
    public String getFileName(){
        return filename;
    }
    public []ResourcedInternalPathData getInternalPaths{
        return internal_paths;
    }
    public ResourceInfo getResourceInfo(){
        return resource_info;
    }
}