package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class EventConfiguration {
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
        return realization_number;
    }
    public SeedSet[] getSeeds(){
        return seeds;
    }
    public SeedSet getSeedSet(String identifier){
        for (SeedSet seedSet : seeds) {
            if (seedSet.getIdentifier()==identifier){
                return seedSet;
            }
        }
        //@TODO: probably should throw exception
        return null;
    }
}