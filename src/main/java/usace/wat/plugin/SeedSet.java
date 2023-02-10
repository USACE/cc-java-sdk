package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class SeedSet {
    @JsonProperty
    private long event_seed;
    @JsonProperty
    private long realization_seed;
    public long getEventSeed(){
        return event_seed;
    }
    public long getRealizationSeed(){
        return realization_seed;
    }
}