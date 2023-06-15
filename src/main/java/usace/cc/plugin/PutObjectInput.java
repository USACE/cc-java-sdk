package usace.cc.plugin;

public class PutObjectInput {
    private String fileName;
    private String fileExtension;
    private StoreType destStoreType;
    private ObjectState objectState;
    private byte[] data;
    private String sourcePath;
    private String destPath;
    public String getFileName(){
        return fileName;
    }
    public String getFileExtension(){
        return fileExtension;
    }
    public StoreType getDestinationStoreType(){
        return destStoreType;
    }
    public ObjectState getObjectState(){
        return objectState;
    }
    public byte[] getData(){
        return data;
    }
    public String getSourcePath(){
        return sourcePath;
    }
    public String getDestinationPath(){
        return destPath;
    }
    /**
     * 
     */
    public PutObjectInput(String fileName, StoreType destStoreType, String sourcePath, String destPath, String fileExtension, ObjectState state, byte[] data){
        this.fileName = fileName;
        this.destStoreType = destStoreType;
        this.sourcePath = sourcePath;
        this.destPath = destPath;
        this.fileExtension = fileExtension;
        this.data = data;
        this.objectState = state;
    }
}
