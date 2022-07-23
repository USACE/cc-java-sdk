package usace.wat.plugin;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;

public class ModelPayload {
    @JsonProperty
    private ModelConfiguration model_configuration;
    @JsonProperty
    private ModelLinks model_links;
    public static ModelPayload readYaml(final File file) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            return mapper.readValue(file, ModelPayload.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelPayload();
    }
    public LinkedDataDescription[] Inputs(){
        return model_links.getLinked_inputs();
    }
    public LinkedDataDescription[] Outputs(){
        return model_links.getRequired_outputs();
    }
    public String ModelName(){
        return model_configuration.ModelName();
    }
    public String ModelAlternative(){
        return model_configuration.ModelAlternative();
    }
}

