package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class FileData {
    @JsonProperty
    private String id;
    @JsonProperty
    private String filename;
    @JsonProperty
    private InternalPathData[] internal_paths;
    public String getId(){
        return id;
    }
    public String getFileName(){
        return filename;
    }
    public InternalPathData[] getInternalPaths(){
        return internal_paths;
    }
}