package usace.cc.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
public class Message {
    @JsonProperty
    private String message;
    public String getMessage(){
        return message;
    }
    public Message(String message){
        this.message = message;
    }
}

