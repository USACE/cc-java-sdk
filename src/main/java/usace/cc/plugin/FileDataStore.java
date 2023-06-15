package usace.cc.plugin;
import java.io.InputStream;

public interface FileDataStore {
    public Boolean Copy(FileDataStore destStore, String srcPath, String destPath);
    public InputStream Get(String path);
    public Boolean Put(InputStream data, String path);
    public Boolean Delete(String path);
}