package usace.wat.plugin;
import java.util.Date;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
public class Message {
    @JsonProperty
    private Status status;
    @JsonProperty
    private short progress;
    @JsonProperty
    private Level level;
    @JsonProperty
    private String message;
    @JsonProperty
    private String sender;
    @JsonProperty
    private String payload_id;
    private DateTime date;
    private Message(){
        message = "";
        level = Level.INFO;
        payload_id = "";
        progress = 0;
        status = Status.COMPUTING;
        date = DateTime.now();
    }
    public static MessageBuilder BuildMessage(){
        MessageBuilder  builder = new MessageBuilder();
        return builder;
    }
    public static class MessageBuilder{
        private Message _message;
        public MessageBuilder(){
            _message = new Message();
        }
        public MessageBuilder withMessage(String message){
            _message.message = message;
            return this;
        }
        public MessageBuilder fromSender(String payloadId){
            _message.payload_id = payloadId;
            return this;
        }
        public MessageBuilder withErrorLevel(Level level){
            _message.level = level;
            return this;
        }
        public MessageBuilder withProgress(short progress){
            _message.progress = progress;
            return this;
        }
        public MessageBuilder withStatus(Status status){
            _message.status = status;
            return this;
        }
        public Message build(){
            return _message;
        }
    }
    enum Status {
	    COMPUTING, //Status = "Computing"
	    FAILED,    //Status = "Failed"
	    SUCCEEDED, //Status = "Succeeded"
    }
    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        PANIC,
        DISABLED,
    }
}

