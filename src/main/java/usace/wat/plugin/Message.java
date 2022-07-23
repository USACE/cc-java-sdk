package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Message {
    @JsonProperty
    private Status status;
    @JsonProperty
    private int8 progress;
    @JsonProperty
    private Level level;
    @JsonProperty
    private String message;
    @JsonProperty
    private String sender;
    @JsonProperty
    private String payload_id;
    //timeStamp time.Time
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

