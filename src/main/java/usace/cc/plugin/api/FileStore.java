package usace.cc.plugin.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.FileVisitor;

public interface FileStore {
    public void copy(FileStore destStore, String srcPath, String destPath) throws DataStoreException;
    public GetObjectOutput get(String path) throws DataStoreException;
    public PutObjectOutput put(InputStream data, String path) throws DataStoreException;
    public void delete(String path) throws DataStoreException;
    public void walk(String path,FileVisitor visitor);
    //public FileSystem getFileSystem(DataStore store, String path) throws IOException;
}