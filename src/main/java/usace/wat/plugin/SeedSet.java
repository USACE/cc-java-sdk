package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class SeedSet {
    @JsonProperty
    private String identifier;
    @JsonProperty
    private long event_seed;
    @JsonProperty
    private long realization_seed;
    public String getIdentifier(){
        return identifier;
    }
    public long getEventSeed(){
        return event_seed;
    }
    public long getRealizationSeed(){
        return realization_seed;
    }
}