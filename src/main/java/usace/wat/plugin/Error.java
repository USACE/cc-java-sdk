package usace.wat.plugin;

import java.util.EnumSet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {
    enum ErrorLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        PANIC,
        DISABLED;
        public static final EnumSet<ErrorLevel> All_Opts = EnumSet.allOf(ErrorLevel.class);
    }
    @JsonProperty
    private String error;
    @JsonProperty
    private ErrorLevel errorlevel;
    public String getError(){
        return error;
    }
    public ErrorLevel getErrorLevel(){
        return errorlevel;
    }
    /*
    func (l ErrorLevel) String() string {
        switch l {
        case INFO:
            return "some information"
        case WARN:
            return "a warning"
        case ERROR:
            return "an error"
        case DEBUG:
            return "a debug statement"
        case FATAL:
            return "a fatal message"
        case PANIC:
            return "a panic'ed state"
        case DISABLED:
            return ""
        default:
            return "unknown level"
        }
    }    
     */

}
