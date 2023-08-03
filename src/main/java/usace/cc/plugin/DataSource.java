package usace.cc.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class DataSource {
    @JsonProperty
    private String Name;
    @JsonProperty
    private String ID;
    @JsonProperty
    private String StoreName;
    @JsonProperty
    private String[] Paths;
    @JsonProperty
    private String[] DataPaths;
    public String getId(){
        return ID;
    }
    public String getName(){
        return Name;
    }
    public String[] getPaths(){
        return Paths;
    }
    public String[] getDataPaths(){
        return DataPaths;
    }
    public String getStoreName(){
        return StoreName;
    }
    public DataSource UpdatePaths(){
        DataSource dest = this;
        PluginManager pm = PluginManager.getInstance();
        dest.Name = pm.SubstitutePath(this.getName());
        if(this.getPaths()!=null){
            for(int j=0; j<this.getPaths().length;j++){
                //pm.LogMessage(new Message("incoming: " + this.getPaths()[j]));
                dest.getPaths()[j] = pm.SubstitutePath(this.getPaths()[j]);
                //pm.LogMessage(new Message("outgoing: " + dest.getPaths()[j]));
            }
        }

        if (this.getDataPaths()!=null){
            for(int j=0; j<this.getDataPaths().length;j++){
                dest.getDataPaths()[j] = pm.SubstitutePath(this.getDataPaths()[j]);
            }
        }

        return dest;
    }
}