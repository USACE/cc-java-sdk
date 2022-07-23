package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class EventConfiguration {
    	EventNumber       int       `json:"event_number"`
	RealizationNumber int       `json:"realization_number"`
	Seeds             []SeedSet `json:"seeds"`
    @JsonProperty
    private int event_number;
    @JsonProperty
    private int realization_number;
    @JsonProperty
    private SeedSet[] seeds;
    public int getEventNumber(){
        return event_number;
    }
    public int getRealizationNumber(){
        return realization_seed;
    }
    public SeedSet[] getSeeds(){
        return seeds;
    }
}