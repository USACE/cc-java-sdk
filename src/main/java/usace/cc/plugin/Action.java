package usace.cc.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Action {
    private String name;
    private String desc;
    private Map<String,DataSource> params;
    @JsonProperty
    public String getName(){
        return name;
    }
    @JsonProperty
    public String getDescription(){
        return desc;
    }
    @JsonProperty
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
