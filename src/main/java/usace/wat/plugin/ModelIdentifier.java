package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ModelIdentifier {
    @JsonProperty
    private Model model;
    @JsonProperty
    private ResourcedFileData[] files;
    public Model getModel(){
        return model;
    }
    public ResourcedFileData[] getFiles(){
        return files;
    }
}