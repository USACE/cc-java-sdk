package usace.wat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Plugin{
    @JsonProperty
    private String name;
    @JsonProperty
    private String image_and_tag;
    @JsonProperty
    private String[] command;
    public String getName(){
        return name;
    }
    public String getImageAndTag(){
        return image_and_tag;
    }
    public String[] getCommand(){
        return command;
    }
}