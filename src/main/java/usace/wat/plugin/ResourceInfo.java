package usace.wat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceInfo {
    @JsonProperty
    private StoreTypes store;
    @JsonProperty
    private String root;
    @JsonProperty
    private String path;
    public String getPath(){
        return path;
    }
    public String getRoot(){
        return root;
    }
    public StoreTypes getStore(){
        return store;
    }
}
