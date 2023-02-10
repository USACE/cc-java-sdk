package usace.wat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Status {
    enum StatusLevel {
	    COMPUTING, //Status = "Computing"
	    FAILED,    //Status = "Failed"
	    SUCCEEDED, //Status = "Succeeded"
    }
    @JsonProperty
    private int progress;
    @JsonProperty
    private StatusLevel status;
    public int getProgress(){
        return progress;
    }
    public StatusLevel getStatus(){
        return status;
    }
}
