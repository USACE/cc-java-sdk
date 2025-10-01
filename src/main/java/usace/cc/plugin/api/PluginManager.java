package usace.cc.plugin.api;
 
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import usace.cc.plugin.api.Error.ErrorLevel;
import usace.cc.plugin.api.IOManager.InvalidDataStoreException;
import usace.cc.plugin.api.action_runner.ActionRunnerRegistry;
import usace.cc.plugin.api.action_runner.ActionRunner;
import usace.cc.plugin.api.action_runner.ActionRunner.ActionRunnerException;
import usace.cc.plugin.api.action_runner.ActionRunnerBase;
import usace.cc.plugin.api.cloud.aws.CcStoreS3;

public final class PluginManager {

    private CcStore cs;
    private Payload payload;
    private Logger logger;
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
        String sender = System.getenv(EnvironmentVariables.CC_PLUGIN_DEFINITION);
        eventIdentifier=System.getenv(EnvironmentVariables.CC_EVENT_IDENTIFIER);
        logger = new Logger(sender, ErrorLevel.WARN);
        cs = new CcStoreS3();
        try {
            this.payload = cs.getPayload();
            this.connectStores(payload.getStores());
            for (Action action : payload.getActions()) {
                this.connectStores(action.getStores());
            }
            substitutePathVariables();
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

    public void setLogLevel(ErrorLevel level){
        logger.setErrorLevel(level);
    }

    public void logMessage(Message message){
        logger.logMessage(message);
    }

    public void logError(Error error){
        logger.logError(error);
    }
    
    public void reportProgress(Status report){
        logger.reportStatus(report);
    }

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

    private void substitutePathVariables(){
        var attrs = this.payload.getAttributes();
        //var attrs = this.payload.getAttributes().merge(this.payload.getActions());
        //substitute attributes from env variables only.
        substitutePayloadAttributes(attrs);


        for (DataSource ds : this.payload.getInputs()){ 
            substitutePaths(ds,attrs);
        }

        for (DataSource ds : this.payload.getOutputs()){ 
            substitutePaths(ds,attrs);
        }

        for (Action action :this.payload.getActions()){
            PayloadAttributes actionAttrs = action.getAttributes();
            //substitute action attributes using payload attributes and env varibles
            substituteAttributes(actionAttrs, attrs);
  
            for (DataSource ds : action.getInputs()){ 
                substitutePaths(ds,actionAttrs);
            }
    
            for (DataSource ds : action.getOutputs()){ 
                substitutePaths(ds,actionAttrs);
            }
            
        }
    }
    //substitute action attributes from env variables and payload attributes.
    private void substitutePayloadAttributes(PayloadAttributes pattrs){
        var attrs = pattrs.getAttributes();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            //var key = entry.getKey();
            var val = entry.getValue();
            if (val instanceof String){
                attrs.put(entry.getKey(),parameterSubstitute((String)val, pattrs, false));
            }            
        }
    }
    //substitute action attributes from env variables and payload attributes.
    private void substituteAttributes(PayloadAttributes pattrs, PayloadAttributes source){
        var attrs = pattrs.getAttributes();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            //var key = entry.getKey();
            var val = entry.getValue();
            if (val instanceof String){
                attrs.put(entry.getKey(),parameterSubstitute((String)val, source, true));
            }            
        }
    }

    //a.k.a. pathssubstitute
    private void substitutePaths(DataSource ds, PayloadAttributes attrs){
        var param = parameterSubstitute(ds.getName(),attrs, true); 
        ds.setName(param);
        
        var paths = ds.getPaths();
        if (paths!=null){
            for (String key : paths.keySet()){
                paths.put(key, parameterSubstitute(paths.get(key), attrs, true));
            }
            ds.setPaths(paths);
        }
       

        var datapathsOpt = ds.getDataPaths();
        if (datapathsOpt.isPresent()){
            var datapaths = datapathsOpt.get();
            for (String key : datapaths.keySet()){
                datapaths.put(key, parameterSubstitute(datapaths.get(key), attrs, true));
            }
            ds.setDataPaths(datapaths);
        }
    }

    private String parameterSubstitute(String param, PayloadAttributes attrs, boolean attrsub) {
        Matcher m = p.matcher(param);
        while(m.find()){
            String result = m.group();
            String[] parts = result.split("::", 0);
            String prefix = parts[0];
            String subname = parts[1];
            switch(prefix){
                case "ENV":
                    String val = System.getenv(subname);
                    param = param.replaceFirst("\\{"+result+"\\}", val);//?
                    m = p.matcher(param);
                break;
                case "ATTR":
                    if(attrsub){
                        Optional<String> optVal = attrs.get(subname);
                        if (optVal.isPresent()){
                            param = param.replaceFirst("\\{"+result+"\\}", optVal.get());//?
                            m = p.matcher(param);
                        } else{
                            //@TODO logging is applied inconsistently
                            logger.logMessage(new Message(String.format("Attribute %s not found", subname)));
                        }                        
                    }else{
                        logger.logMessage(new Message(String.format("Attribute substitution is not allowed in this case %s not used", subname)));
                    }

                break;
            }
        }
        return param;
    }
}