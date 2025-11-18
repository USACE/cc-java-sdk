package usace.cc.plugin.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.cloud.aws.FileStoreS3;

public class IOManager {

    //IO Manager Error Types
    public static class InvalidDataSourceException extends RuntimeException {
        public InvalidDataSourceException(String message) {
            super(message);
        }

        public InvalidDataSourceException(Exception ex){
            super(ex);
        }
    }

    public static class InvalidDataStoreException extends Exception {
        public InvalidDataStoreException(String message) {
            super(message);
        }
        
        public InvalidDataStoreException(Exception ex){
            super(ex);
        }
    }

    @JsonProperty
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Map<String, Object> attributes;
    //private PayloadAttributes attributes;

    @JsonProperty
    @JsonIgnoreProperties(ignoreUnknown = true)
    private DataStore[] stores;
    
    @JsonProperty
    @JsonIgnoreProperties(ignoreUnknown = true)
    private DataSource[] inputs;
    
    @JsonProperty
    @JsonIgnoreProperties(ignoreUnknown = true)
    private DataSource[] outputs;

    private IOManager parent;
    
    public PayloadAttributes getAttributes(){
        return new PayloadAttributes(attributes);
    }

    public void setAttributes(Map<String,Object> attrs){
        this.attributes=attrs;
    }
    
    public DataStore[] getStores(){
        return this.stores;
    }

    public DataSource[] getInputs(){
        return this.inputs;
    }

    public DataSource[] getOutputs(){
        return this.outputs;
    }

    public void setParent(IOManager parent){
        this.parent=parent;
    }


    /**
     * Retrieves a {@link DataStore} by its name.
     * <p>
     * This method searches the current list of data stores for one with a matching name.
     * If not found and a parent is defined, it will recursively search the parent.
     * </p>
     *
     * @param name the name of the data store to retrieve
     * @return an {@code Optional} containing the matching {@code DataStore}, or
     *         {@code Optional.empty()} if no matching store is found in this instance
     *         or its parent chain
     */
    public Optional<DataStore> getStore(String name){
        for (DataStore store:this.stores){
            if (name.equals(store.getName())){
                return Optional.of(store);
            }
        }
        if (this.parent!=null){
            return parent.getStore(name);
        }
        return Optional.empty();
    }
    

    /**
     * Retrieves a {@link DataSource} based on the provided input criteria.
     * <p>
     * This method selects from input sources, output sources, or both depending on the
     * {@link GetDataSourceInput#getDataSourceIOType()} value. It then searches for a
     * {@code DataSource} with a matching name. If no match is found locally and a parent
     * exists, the search will continue recursively in the parent.
     * </p>
     *
     * @param gdsi an object encapsulating the data source name and the I/O type to search
     * @return an {@code Optional} containing the matching {@code DataSource}, or {@code Optional.empty()}
     *         if no such data source exists in this instance or its parent chain
     * @throws InvalidDataSourceException runtime exception if the {@code DataSourceIOType} in the input is not recognized
     */
    public Optional<DataSource> getDataSource(GetDataSourceInput gdsi) throws InvalidDataSourceException{
        DataSource[] sources;
        switch (gdsi.getDataSourceIOType()) {
            case INPUT:
                sources = this.inputs;
                break;
            case OUTPUT:
                sources = this.outputs;
                break;
            case ANY:
                sources = Arrays.copyOf(this.inputs,this.inputs.length + this.outputs.length);
                System.arraycopy(this.outputs, 0, sources, this.inputs.length, this.outputs.length);
                break;
            default:
                throw new InvalidDataSourceException("data source input type not recognized");
        }

        for(DataSource ds : sources){
            if(ds.getName().equals(gdsi.getDataSourceName())){
                return Optional.of(ds);
            }
        }

        if (this.parent!=null){
            return this.parent.getDataSource(gdsi);
        }

        return Optional.empty();
    }

    /**
     * Retrieves an input {@link DataSource} by name.
     * <p>
     * This is a convenience method that constructs a {@link GetDataSourceInput} with the specified
     * name and an I/O type of {@code INPUT}, then delegates to {@link #getDataSource(GetDataSourceInput)}.
     * </p>
     *
     * @param name the name of the input data source to retrieve
     * @return an {@code Optional} containing the matching input {@code DataSource}, or
     *         {@code Optional.empty()} if no matching source is found locally or in a parent
     * @throws InvalidDataSourceException if the underlying data source lookup encounters an error
     */
    public Optional<DataSource> getInputDataSource(String name) throws InvalidDataSourceException {
        var gdsi = new GetDataSourceInput(name, DataSourceIOType.INPUT);
        return getDataSource(gdsi);
    }

    /**
     * Retrieves an output {@link DataSource} by name.
     * <p>
     * This is a convenience method that constructs a {@link GetDataSourceInput} with the specified
     * name and an I/O type of {@code OUTPUT}, then delegates to {@link #getDataSource(GetDataSourceInput)}.
     * </p>
     *
     * @param name the name of the output data source to retrieve
     * @return an {@code Optional} containing the matching output {@code DataSource}, or
     *         {@code Optional.empty()} if no matching source is found locally or in a parent
     * @throws InvalidDataSourceException if the underlying data source lookup encounters an error
     */
    public Optional<DataSource> getOutputDataSource(String name) throws InvalidDataSourceException {
        var gdsi = new GetDataSourceInput(name, DataSourceIOType.OUTPUT);
        return getDataSource(gdsi);
    }

    public void copyFilesToLocal(String dataSourceName, String pathkey, String localPath) throws IOException{
        Path source=Paths.get("model-library/grids");
        Path target=Paths.get("/data");
        var input = new GetDataSourceInput(dataSourceName, DataSourceIOType.INPUT);
        Optional<DataSource> sourceOpt = getDataSource(input);
        if (sourceOpt.isPresent()){
            var datasource = sourceOpt.get();
            var storeOpt = getStore(datasource.getStoreName());
            if (storeOpt.isPresent()){
                var store = storeOpt.get();
                FileStoreS3 fss3 = (FileStoreS3)store.getSession();
                //var fs = fss3.getFileSystem(store,"model-library");
                 System.out.println(fss3);
            }
        }
        
        // try (Stream<Path> walk = Files.walk(source)) {
        //     walk.forEach(sourcePath -> {
        //         Path targetPath = target.resolve(source.relativize(sourcePath));
        //         //try {
        //             System.out.println(targetPath);
        //             // Use Files.copy for both files and directories, handling options carefully
        //             //Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        //         // } catch (IOException e) {
        //         //     System.err.println("Could not copy " + sourcePath + ": " + e.getMessage());
        //         //}
        //     });
        // }

    }

    //@TODO....I include a data path here
    /**
     * Copies a file from an input {@link DataSource} to a local file path.
     * <p>
     * This method locates the input data source by name, retrieves the input stream
     * for the specified path key, reads the file's contents, and writes them to the
     * specified local file path.
     * </p>
     *
     * @param dataSourceName the name of the input data source
     * @param pathkey the key or path identifying the file within the data source
     * @param localPath the path on the local filesystem where the file will be written
     * @throws InvalidDataSourceException if the data source is not found or if an error occurs while accessing it
     * @throws IOException if an I/O error occurs during reading from the source or writing to the local file
     * @throws DataStoreException if an error occurs while interacting with the data store
     */
    public void copyFileToLocal(String dataSourceName, String pathkey, String localPath) throws IOException, InvalidDataSourceException, DataStoreException{
        Optional<DataSource> indsOpt = getDataSource(new GetDataSourceInput(dataSourceName, DataSourceIOType.INPUT));
        if (!indsOpt.isPresent()){
            throw new InvalidDataSourceException("Data source not found");
        }

        DataSource inds = indsOpt.get();
        
        try (InputStream is = getInputStream(inds, pathkey)) {
            byte[] bytes = is.readAllBytes();
            File outfile = new File(localPath);
           
            //
            File parentDir = outfile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if(parentDir.mkdirs()){
                    if (!outfile.createNewFile()){
                        throw new DataStoreException(String.format("unable to create file: %s",localPath));
                    }
                } else {
                    throw new DataStoreException(String.format("unable to create full path for %s",localPath));
                }
            }
           //
            
            try(OutputStream writer = new FileOutputStream(outfile)){
                writer.write(bytes);
            } 
        }
    }

    /**
     * Retrieves the contents of a file from an input {@link DataSource} as a byte array.
     * <p>
     * This method locates the data source by name and attempts to read the contents of the file
     * identified by the given {@code pathName}. If the data source is not found, or an error occurs
     * while accessing it, an exception is thrown. The {@code dataPathName} is included for error
     * context but not used directly in the lookup.
     * </p>
     *
     * @param dataSourceName the name of the input data source
     * @param pathName the key or identifier of the file within the data source
     * @param dataPathName an additional path descriptor for context in error reporting
     * @return a byte array containing the contents of the specified file
     * @throws InvalidDataSourceException if the data source is not found or cannot be accessed
     * @throws IOException if an I/O error occurs while reading from the data source
     * @throws DataStoreException if an error occurs while interacting with the data store
     */
    public byte[] get(String dataSourceName, String pathName, String dataPathName) throws IOException, InvalidDataSourceException, DataStoreException{
        //@TODO what do do with dataPathName
        Optional<DataSource> dsOpt = this.getDataSource(new GetDataSourceInput(dataSourceName, DataSourceIOType.INPUT));
        if (dsOpt.isPresent()){
            var ds = dsOpt.get();
            return getInputStream(ds, pathName).readAllBytes();
        }
        throw new InvalidDataSourceException(String.format("invalid data source. Name: %s, Path: %s, DataPath: %s",dataSourceName,pathName,dataPathName));
    }

     /**
     * Retrieves the contents of a file from an input {@link DataSource} as a byte array.
     * <p>
     * This method locates the data source by name and attempts to read the contents of the file
     * identified by the given {@code pathName}. If the data source is not found, or an error occurs
     * while accessing it, an exception is thrown. The {@code dataPathName} is included for error
     * context but not used directly in the lookup.
     * </p>
     *
     * @param dataSource the the input data source
     * @param pathName the key or identifier of the file within the data source
     * @param dataPathName an additional path descriptor for context in error reporting
     * @return a byte array containing the contents of the specified file
     * @throws InvalidDataSourceException if the data source is not found or cannot be accessed
     * @throws IOException if an I/O error occurs while reading from the data source
     * @throws DataStoreException if an error occurs while interacting with the data store
     */
    public byte[] get(DataSource ds, String pathName, String dataPathName) throws IOException, InvalidDataSourceException, DataStoreException{
            return getInputStream(ds, pathName).readAllBytes();
    }

    /**
     * Retrieves an {@link InputStream} from the given {@link DataSource} using the specified path key.
     * <p>
     * This method attempts to obtain a {@code FileDataStore} session from the data source's store name,
     * and then retrieves the corresponding input stream using the provided {@code pathKey}.
     * </p>
     *
     * @param dataSource the data source from which to retrieve the input stream
     * @param pathKey the key or identifier used to locate the desired file within the store
     * @return an {@code InputStream} for reading the data associated with the given key
     * @throws InvalidDataSourceException if the store session cannot be obtained, if the store is not
     *         present, or if an underlying {@code InvalidDataStoreException} occurs
     */
    public InputStream getInputStream(DataSource dataSource, String pathKey) throws InvalidDataSourceException, DataStoreException{
        try{
            Optional<FileStore> fdsOpt = getStoreSession(dataSource.getStoreName());
            if(fdsOpt.isPresent()){
                var fds = fdsOpt.get();
                String filepath=dataSource.getPaths().get(pathKey);
                return fds.get(filepath).getContent();
            }
            throw new InvalidDataSourceException("Unable to get input stream from the data source");
        } catch(InvalidDataStoreException ex) {
            throw new InvalidDataSourceException(ex);
        }
    }

    /**
     * Writes the provided byte array to a specified path in a named output data source.
     *
     * <p>This method looks up the data source by name and attempts to retrieve a corresponding
     * {@code FileDataStore} session. If both are found, it writes the data to the given path
     * within that data store. If either the data source or the data store is not found,
     * an appropriate checked exception is thrown.</p>
     *
     * @param data the data to write
     * @param dataSourceName the name of the output data source to target
     * @param pathKey the path within the data store to write the data to
     * @param dataPathName currently unused, reserved for future expansion or routing
     *
     * @throws IOException if an I/O error occurs during the write
     * @throws InvalidDataSourceException if the specified data source cannot be found
     * @throws InvalidDataStoreException if the data store session for the data source cannot be obtained
     * @throws DataStoreException if an error occurs while interacting with the data store
     */
    public PutObjectOutput put(byte[] data, String dataSourceName, String pathKey, String dataPathName) throws IOException, InvalidDataSourceException, InvalidDataStoreException, DataStoreException{
        Optional<DataSource> dsOpt = this.getDataSource(new GetDataSourceInput(dataSourceName, DataSourceIOType.OUTPUT));
        if (dsOpt.isPresent()){
            var ds = dsOpt.get();
            Optional<FileStore> fdsOpt = getStoreSession(ds.getStoreName());
            if (fdsOpt.isPresent()){
                var fds = fdsOpt.get();
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                String filepath=ds.getPaths().get(pathKey);
                return fds.put(bais, filepath);
            } else {
                throw new InvalidDataStoreException("Datastore not found");
            }
        } else {
            throw new InvalidDataSourceException("Datasource not found");
        }
    }

    /**
     * Copies a local file to a remote data store.
     *
     * <p>This method retrieves a {@link DataSource} configured for output using the given
     * destination name, then locates the associated {@link FileStore} session. 
     * It reads the local file from the specified {@code localPath} and uploads its contents 
     * to the remote store under the provided {@code pathKey}.</p>
     *
     * @param destinationName the name of the remote destination to which the file will be copied
     * @param pathKey the key (path) under which the file will be stored remotely
     * @param localPath the file system path to the local file that needs to be copied
     * @throws InvalidDataSourceException if the specified data source is invalid or cannot be retrieved
     * @throws InvalidDataStoreException if the data store session cannot be established or is invalid
     * @throws IOException if an I/O error occurs while reading the local file or writing to the remote store
     * @throws DataStoreException if an error occurs while interacting with the data store
     */
    public void copyFileToRemote(String destinationName, String pathKey, String localPath) throws InvalidDataSourceException, InvalidDataStoreException, IOException, DataStoreException{
        Optional<DataSource> dsOpt = this.getDataSource(new GetDataSourceInput(destinationName, DataSourceIOType.OUTPUT));
        if(dsOpt.isPresent()){
            var ds = dsOpt.get();
            Optional<FileStore> fdsOpt = getStoreSession(ds.getStoreName());
            if(fdsOpt.isPresent()){
                var fdstore = fdsOpt.get();
                File localFile = new File(localPath);
                InputStream reader = new FileInputStream(localFile);
                String filepath=ds.getPaths().get(pathKey);
                fdstore.put(reader, filepath);
            }    
        } 
    }


    /**
     * Retrieves a session object of type {@code T} from a data store with the specified name.
     * <p>
     * This method searches through the available {@code DataStore} instances to find one
     * matching the given {@code name}. If found, it attempts to retrieve and cast the session
     * object to the desired type {@code T}.
     * </p>
     * 
     * @param <T>   The expected type of the session object.
     * @param name  The name of the data store to retrieve the session from.
     * @return An {@code Optional} containing the session object if present and of the correct type,
     *         or {@code Optional.empty()} if no matching store is found or the session is {@code null}.
     * @throws InvalidDataStoreException if the session cannot be cast to the specified type {@code T}.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getStoreSession(String name) throws InvalidDataStoreException{
        var storeOpt = getStore(name);
        if(storeOpt.isPresent()){
            var store = storeOpt.get();
            try{
                T session = (T)store.getSession();
                if (session==null){
                    return Optional.empty();    
                }
                return Optional.of(session);
            } catch(ClassCastException ex){
                throw new InvalidDataStoreException(ex);
            }
        }
        return Optional.empty();
    }
}
