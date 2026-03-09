package usace.cc.plugin.api;
 
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import usace.cc.plugin.api.IOManager.InvalidDataStoreException;
import usace.cc.plugin.api.action_runner.ActionRunnerRegistry;
import usace.cc.plugin.api.action_runner.ActionRunner;
import usace.cc.plugin.api.action_runner.ActionRunner.ActionRunnerException;
import usace.cc.plugin.api.action_runner.ActionRunnerBase;
import usace.cc.plugin.api.cloud.aws.CcStoreS3;
import usace.cc.plugin.api.local.CcStoreLocal;

public final class PluginManager {

    private CcStore cs;
    private Payload payload;
    public Logger log;
    private String eventIdentifier;
    private static PluginManager instance = null;
    //private boolean hasUpdatedPaths = false;
    private Pattern p;
    private final String pathPattern = "(?<=\\{).+?(?=\\})";
    
    public static PluginManager getInstance() throws InvalidDataStoreException{
        if (instance==null){
            instance = new PluginManager();
        }
        return instance;
    }
    
    
    private PluginManager() throws InvalidDataStoreException{
        p = Pattern.compile(pathPattern);
        //String sender = System.getenv(EnvironmentVariables.CC_PLUGIN_DEFINITION);
        eventIdentifier=System.getenv(EnvironmentVariables.CC_EVENT_IDENTIFIER);
        this.log = new Logger("PluginManager");

        String storeType = System.getenv(EnvironmentVariables.CC_STORE_TYPE);
        if (storeType == null || storeType.isEmpty()) {
            storeType = "S3"; // Default to S3 if not specified
        }
        switch (storeType) {
            case "FS":
                cs = new CcStoreLocal();
                break;
            case "S3":
                cs = new CcStoreS3();
                break;
            default:
                throw new InvalidDataStoreException(
                    new IllegalArgumentException("Unsupported store type: " + storeType));
        }

        try {
            this.payload = cs.getPayload();
            this.connectStores(payload.getStores());
            for (Action action : payload.getActions()) {
                this.connectStores(action.getStores());
            }
            substituteVariables();
        } catch (Exception e) {
            throw new InvalidDataStoreException(e);
        }
        
    }
    
    public Payload getPayload(){
        return payload;
    }

    public String getEventIdentifier() {
        return this.eventIdentifier;
    }

    // public void log(String msg, Object ...kvps){
    //     this.log(msg, kvps);    
    // }

    // public void setLogLevel(ErrorLevel level){
    //     logger.setErrorLevel(level);
    // }

    // public void logMessage(Message message){
    //     logger.logMessage(message);
    // }

    // public void logError(Error error){
    //     logger.logError(error);
    // }
    
    // public void reportProgress(Status report){
    //     logger.reportStatus(report);
    // }

    public void RunActions() throws ActionRunnerException{
        for (Action action:this.getPayload().getActions()){
            Optional<Class<? extends ActionRunner>> runnerClassOpt = ActionRunnerRegistry.getInstance().getActionRunnerClass(action.getName());
            if(runnerClassOpt.isPresent()){
                try{
                    Constructor<? extends ActionRunner> constructor = runnerClassOpt.get().getConstructor();
                    ActionRunner runner = constructor.newInstance();
                    if(runner instanceof ActionRunnerBase){
                        ActionRunnerBase baseRunner =(ActionRunnerBase)runner;
                        baseRunner.setPm(instance);
                        baseRunner.setAction(action);
                        baseRunner.setName(action.getName()); 
                        baseRunner.log=new Logger(baseRunner.getName());
                    }
                    runner.Run();
                } catch(Exception ex){
                    throw new ActionRunnerException(ex);
                }
            } else {
                throw new ActionRunnerException(String.format("invalid action name: %s",action.getName()));
            }
        }
    }

    //Private methods
    private void connectStores(DataStore[] stores) throws Exception{
        if(stores !=null){
            for (DataStore store : stores){
                Optional<ConnectionDataStore> dsOpt = DataStoreTypeRegistry.newStore(store.getStoreType());
                if (dsOpt.isPresent()){
                    var dsInstance = dsOpt.get();
                    var conn = dsInstance.connect(store);
                    store.setSession(conn);
                }
            }
        }
    }

    private void substituteVariables(){
        var newattrs = substituteAttributes(payload.getAttributes().getAttributes(), false);
        this.payload.setAttributes(newattrs);

        var attrs = this.payload.getAttributes();

        for (DataSource ds : this.payload.getInputs()){ 
            substitutePaths(ds,attrs);
        }

        for (DataSource ds : this.payload.getOutputs()){ 
            substitutePaths(ds,attrs);
        }

        for (Action action :this.payload.getActions()){
             var newActionAttr = substituteAttributes(action.getAttributes().getAttributes(),true);
             action.setAttributes(newActionAttr);

            //create a merged map for action path substitution
            var mergedAttr = attrs.merge(action.getAttributes());
  
            for (DataSource ds : action.getInputs()){ 
                substitutePaths(ds,mergedAttr);
            }
    
            for (DataSource ds : action.getOutputs()){ 
                substitutePaths(ds,mergedAttr);
            }
        }
    }


    private Map<String,Object> substituteAttributes(Map<String,Object> attrs, boolean allowAttrSub){
        Map<String,Object> newparams = new HashMap<>();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            //var key = entry.getKey();
            var val = entry.getValue();
            if (val instanceof String){
                var results=Substituter.parameterSubstitution(entry.getKey(),(String)val, attrs,allowAttrSub);
                newparams.putAll(results);
            } else if (val instanceof Map){
                var lvals = (Map<String,Object>)val;
                var mapparams = substituteAttributes(lvals,allowAttrSub);
                newparams.put(entry.getKey(), mapparams);
            } else if (val instanceof List<?>){
                var lvals = (List<?>)val;
                var newlist = new ArrayList<Object>();
                for(Object lval : lvals){
                    if (lval instanceof String){
                       var results=Substituter.parameterSubstitution(entry.getKey(),(String)lval, attrs,allowAttrSub);
                       for (Map.Entry<String,String> result: results.entrySet()){
                            newlist.add(result.getValue());
                       }
                    } else {
                        newlist.add(lval);
                    }
                }
                newparams.put(entry.getKey(), newlist);
            } else {
                newparams.put(entry.getKey(), val);
            }            
        }
        return newparams;
    }

    //a.k.a. pathssubstitute
    private void substitutePaths(DataSource ds, PayloadAttributes attrs){
        var param = Substituter.parameterSubstitution("name",ds.getName(),attrs.getAttributes(),true); 
        ds.setName(param.get("name"));
        
        var paths = ds.getPaths();
        if (paths!=null){
            var keys = Set.copyOf(paths.keySet());
            for (String key : keys){
                var newPaths = Substituter.parameterSubstitution(key,paths.get(key), attrs.getAttributes(),true);
                paths.remove(key);
                paths.putAll(newPaths);
            }
            ds.setPaths(paths);
        }
       

        var datapathsOpt = ds.getDataPaths();
        if (datapathsOpt.isPresent()){
            var datapaths = datapathsOpt.get();
            var datakeys = Set.copyOf(datapaths.keySet());
            for (String key : datakeys){
                var newPaths = Substituter.parameterSubstitution(key,datapaths.get(key), attrs.getAttributes(),true);
                datapaths.remove(key);
                datapaths.putAll(newPaths);
            }
            ds.setDataPaths(datapaths);
        }
    }

    // private void substitutePaths(DataSource ds, PayloadAttributes attrs){
    //     var param = parameterSubstitute(ds.getName(),attrs,true); 
    //     ds.setName(param);
        
    //     var paths = ds.getPaths();
    //     if (paths!=null){
    //         for (String key : paths.keySet()){
    //             paths.put(key, parameterSubstitute(paths.get(key), attrs,true));
    //         }
    //         ds.setPaths(paths);
    //     }
       

    //     var datapathsOpt = ds.getDataPaths();
    //     if (datapathsOpt.isPresent()){
    //         var datapaths = datapathsOpt.get();
    //         for (String key : datapaths.keySet()){
    //             datapaths.put(key, parameterSubstitute(datapaths.get(key), attrs,true));
    //         }
    //         ds.setDataPaths(datapaths);
    //     }
    // }

    // private String parameterSubstitute(String param, PayloadAttributes attrs, boolean allowAttrSub) {
    //     Matcher m = p.matcher(param);
    //     while(m.find()){
    //         String result = m.group();
    //         String[] parts = result.split("::", 0);
    //         String prefix = parts[0];
    //         String subname = parts[1];
    //         switch(prefix){
    //             case "ENV":
    //                 String val = System.getenv(subname);
    //                 param = param.replaceFirst("\\{"+result+"\\}", val);//?
    //                 m = p.matcher(param);
    //             break;
    //             case "ATTR":
    //                 if (allowAttrSub){
    //                     Optional<String> optVal = attrs.get(subname);
    //                     if (optVal.isPresent()){
    //                         param = param.replaceFirst("\\{"+result+"\\}", optVal.get());//?
    //                         m = p.matcher(param);
    //                     } else{
    //                         //@TODO logging is applied inconsistently
    //                         logger.logMessage(new Message(String.format("Attribute %s not found", subname)));
    //                     }
    //                 }
    //             break;
    //         }
    //     }
    //     return param;
    // }
}