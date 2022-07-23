package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class SeedSet {
    @JsonProperty
    private String identifier;
    @JsonProperty
    private int64 event_seed;
    @JsonProperty
    private int64 realization_seed;
    public String getIdentifier(){
        return identifier;
    }
    public int64 getEventSeed(){
        return event_seed;
    }
    public int64 getRealizationSeed(){
        return realization_seed;
    }
}