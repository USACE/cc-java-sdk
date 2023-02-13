package usace.wat.plugin;
 
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Map;

import usace.wat.plugin.Error.ErrorLevel;

public final class PluginManager {
    private CcStore cs;
    private String _manifestId;
    private Payload _payload;
    private Logger _logger;
    public PluginManager(){
        String sender = System.getenv(EnvironmentVariables.CC_PLUGIN_DEFINITION);
        _logger = new Logger(sender, ErrorLevel.WARN);
        _manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        cs = new CcStoreS3();
        try {
            _payload = cs.GetPayload();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public Payload getPayload(){
        return _payload;
    }
    public FileDataStore getFileStore(String storeName){
        return (FileDataStore) findDataStore(storeName);//check for nil?
    }
    public DataStore getStore(String storeName){
        return findDataStore(storeName);
    }
    public DataSource getInputDataSource(String name){
        return findDataSource(name, getInputDataSources());
    }
    public DataSource getOutputDataSource(String name){
        return findDataSource(name, getOutputDataSources());
    }
    public DataSource[] getInputDataSources(){
        return _payload.getInputs();
    }
    public DataSource[] getOutputDataSources(){
        return _payload.getOutputs();
    }
    public byte[] getFile(DataSource ds, int path){
        FileDataStore store = getFileStore(ds.getStoreName());
        FileInputStream reader = store.Get(ds.getPaths()[path]);
        byte[] data;
        try {
            data = new byte[(int) reader.getChannel().size()];
            reader.read(data);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public void setLogLevel(ErrorLevel level){
        _logger.setErrorLevel(level);
    }
    public void LogMessage(Message message){
        _logger.LogMessage(message);
    }
    public void LogError(Error error){
        _logger.LogError(error);
    }
    public void ReportProgress(Status report){
        _logger.ReportStatus(report);
    }
    public int EventNumber(){
        Object result = _payload.getAttributes().get(EnvironmentVariables.CC_EVENT_NUMBER);
        int eventNumber = Integer.parseInt(result.toString());
        return eventNumber;
    }
    private DataSource findDataSource(String name, DataSource[] dataSources){
        for (DataSource dataSource : dataSources) {
            if (dataSource.getName().equalsIgnoreCase(name)){
                return dataSource;
            }
        }
        return null;
    }
    private DataStore findDataStore(String name){
        for (DataStore dataStore : _payload.getStores()) {
            if (dataStore.getName().equalsIgnoreCase(name)){
                return dataStore;
            }
        }
        return null;
    }
}