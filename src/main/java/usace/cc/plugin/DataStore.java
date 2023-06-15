package usace.cc.plugin;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
public class DataStore {
    @JsonProperty
    private String Name;
    @JsonProperty
    private String ID;
    @JsonProperty
    private Map<String, String> Parameters;
    @JsonProperty
    private StoreType StoreType;
    @JsonProperty
    private String DsProfile;
    @JsonProperty
    private Object Session;
    
    public String getName(){
        return Name;
    }
    public String getId(){
        return ID;
    }
    public StoreType getStoreType(){
        return StoreType;
    }
    public Map<String, String> getParameters(){
        return Parameters;
    }
    public String getDsProfile(){
        return DsProfile;
    }
    public Object getSession(){
        return Session;
    }
    public void setSession(Object session){
        this.Session = session;
    }

}