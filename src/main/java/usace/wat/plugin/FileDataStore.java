package usace.wat.plugin;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public interface FileDataStore {
    public Boolean Copy(FileDataStore destStore, String srcPath, String destPath);
    public FileInputStream Get(String path);
    public Boolean Put(InputStreamReader data, String path);
    public Boolean Delete(String path);
}