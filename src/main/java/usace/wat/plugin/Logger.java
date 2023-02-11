package usace.wat.plugin;

import java.time.LocalDate;

import usace.wat.plugin.Error.ErrorLevel;

public class Logger {
    //this is an aggregator, it is anticipated that this will get replaced but the api will remain.
    private ErrorLevel errorLevel;
    private String sender;
    public Logger(String sender, ErrorLevel level){
        this.sender = sender;
        this.errorLevel = level;
    }
    public void setErrorLevel(ErrorLevel level){
        this.errorLevel = level;
    }
    public void LogMessage(Message message){
        String line = this.sender + ":" + LocalDate.now() + "\n\t" + message.getMessage() + "\n";
        System.out.println(line);
    }
    public void LogError(Error error){
        if (error.getErrorLevel().compareTo(this.errorLevel)>=0){
            String line = sender + "issues a " + error.getErrorLevel().toString() + " error:" + LocalDate.now() + "\n\t" + error.getError() + "\n";
            System.out.println(line);
        }
        
        
    }
    public void ReportStatus(Status report){
        String line = this.sender + ":" + report.getStatus().toString() + ":" + LocalDate.now() + "\n\t" + report.getProgress() + " percent complete." + "\n";
        System.out.println(line);
    }
}
