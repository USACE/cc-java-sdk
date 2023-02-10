package usace.wat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
public class Message {
    @JsonProperty
    private String message;
    public String getMessage(){
        return message;
    }

}

