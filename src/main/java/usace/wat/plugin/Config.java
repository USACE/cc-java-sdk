package usace.wat.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Config {
    @JsonProperty
    public AWSConfig[] aws_configs;
    public AWSConfig PrimaryConfig(){
        //loop through and find the "primary config for where payloads are stored."
        for (AWSConfig awsConfig : aws_configs) {
            if(awsConfig.is_primary_config){
                return awsConfig;
            }
        }
        return aws_configs[0];
    }
}