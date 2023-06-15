package usace.cc.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Config {
    @JsonProperty
    public AWSConfig[] aws_configs;
}