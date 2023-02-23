package usace.wat.plugin;
 
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import usace.wat.plugin.Error.ErrorLevel;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginManager {
    private CcStore cs;
    private String _manifestId;
    private Payload _payload;
    private Logger _logger;
    Pattern p;
    public PluginManager(){
        p = Pattern.compile("(?<=\\{).+?(?=\\})");
        String sender = System.getenv(EnvironmentVariables.CC_PLUGIN_DEFINITION);
        _logger = new Logger(sender, ErrorLevel.WARN);
        _manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        cs = new CcStoreS3();
        try {
            _payload = cs.GetPayload();
            int i = 0;
            for (DataStore store : _payload.getStores()) {
                switch (store.getStoreType()){
                    case S3:
                        store.setSession(new FileDataStoreS3(store));
                        _payload.setStore(i, store);
                        break;
                    case WS://does java work with fallthrough?
                    case RDBMS:
                        System.out.println("WS and RDBMS session instantiation is the responsibility of the plugin.");
                        break;
                    default:
                        System.out.println("Invalid Store type");//what type was provided?
                        break;
                }
                i ++;
            }
            substitutePathVariables();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        
    }
    private void substitutePathVariables() {
        for (DataSource ds : _payload.getInputs()){
            for(String path : ds.getPaths()){
                path = substituteDataSourcePath(path);//is this a pointer? test
            }
            
        }
        for (DataSource ds : _payload.getOutputs()){
            for(String path: ds.getPaths()){
                path = substituteDataSourcePath(path);//is this a pointer? test
            }
        }
    }
    private String substituteDataSourcePath(String path) {
        Matcher m = p.matcher(path);
        while(m.find()){
            String result = m.group();
            String[] parts = result.split("::", 0);
            String prefix = parts[0];
            switch(prefix){
                case "ENV":
                    String val = System.getenv(parts[1]);
                    path = path.replaceFirst("\\{"+result+"\\}", val);//?
                    m = p.matcher(path);
                break;
                case "ATTR":
                    String valattr = _payload.getAttributes().get(parts[1]).toString();
                    path = path.replaceFirst("\\{"+result+"\\}", valattr);//?
                    m = p.matcher(path);
                break;
                default:
                break;
            }
        }
        return path;
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
        InputStream reader = store.Get(ds.getPaths()[path]);
        byte[] data;
        try {
            data = reader.readAllBytes();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean putFile(byte[] data, DataSource ds, int path){
        FileDataStore store = getFileStore(ds.getStoreName());
        return store.Put(new ByteArrayInputStream(data), ds.getPaths()[path]);
    }
    public boolean fileWriter(InputStream inputstream, DataSource destDs, int destPath){
        FileDataStore store = getFileStore(destDs.getStoreName());
        return store.Put(inputstream, destDs.getPaths()[destPath]);
    }
    public InputStream fileReader(DataSource ds, int path){
        FileDataStore store = getFileStore(ds.getStoreName());
        return store.Get(ds.getPaths()[path]);
    }
    public InputStream fileReaderByName(String dataSourceName, int path){
        DataSource ds = findDataSource(dataSourceName, getInputDataSources());
        return fileReader(ds, path);
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