package usace.wat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceInfo {
    @JsonProperty
    private String scheme;
    @JsonProperty
    private String authority;
    @JsonProperty
    private String fragment;
    public String getFilePath(){
        return authority + fragment;
    }
    public String getAuthority(){
        return authority;
    }
    public String getFragment(){
        return fragment;
    }
}
