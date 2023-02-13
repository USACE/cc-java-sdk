package usace.wat.plugin;

public class GetObjectInput {
    private String fileName;
    private String fileExtension;
    private StoreType sourceStoreType;
    private String sourceRootPath;
    public String getFileName(){
        return fileName;
    }
    public String getFileExtension(){
        return fileExtension;
    }
    public StoreType getSourceStoreType(){
        return sourceStoreType;
    }
    public String getSourceRootPath(){
        return sourceRootPath;
    }
    /**
     * 
     */
    public GetObjectInput(String fileName, StoreType sourceStoreType, String sourceRootPath, String fileExtension){
        this.fileName = fileName;
        this.sourceStoreType = sourceStoreType;
        this.sourceRootPath = sourceRootPath;
        this.fileExtension = fileExtension;
    }
}