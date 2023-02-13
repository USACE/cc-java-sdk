package usace.wat.plugin;


public class PullObjectInput {
    private String fileName;
    private String fileExtension;
    private StoreType sourceStoreType;
    private String sourceRootPath;
    private String destRootPath;
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
    public String getDestRootPath(){
        return destRootPath;
    }
    /**
     * 
     */
    public PullObjectInput(String fileName, StoreType sourceStoreType, String sourceRootPath, String destRootPath, String fileExtension){
        this.fileName = fileName;
        this.sourceStoreType = sourceStoreType;
        this.sourceRootPath = sourceRootPath;
        this.destRootPath = destRootPath;
        this.fileExtension = fileExtension;
    }
}

