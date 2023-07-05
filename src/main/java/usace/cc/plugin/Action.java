package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Action {
    @JsonProperty
    private String name;
    @JsonProperty
    private String desc;
    @JsonProperty
    private Map<String,DataSource> params;
    public String getName(){
        return name;
    }
    
    public String getDescription(){
        return desc;
    }
    
    public Map<String,DataSource> getParameters(){
        return params;
    }
    public void UpdateActionPaths(){
        PluginManager pm = PluginManager.getInstance();
        this.name = pm.SubstitutePath(this.name);
        this.desc = pm.SubstitutePath(this.desc);
        if(params!=null){
            for(Map.Entry<String, DataSource> apb : params.entrySet()){
                params.replace(apb.getKey(),apb.getValue().UpdatePaths());
            }            
        }

    }
}
