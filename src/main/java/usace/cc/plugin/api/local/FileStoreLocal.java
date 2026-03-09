package usace.cc.plugin.api.local;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import usace.cc.plugin.api.ConnectionDataStore;
import usace.cc.plugin.api.DataStore;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.EnvironmentVariables;
import usace.cc.plugin.api.FileStore;
import usace.cc.plugin.api.GetObjectOutput;
import usace.cc.plugin.api.PutObjectOutput;
import usace.cc.plugin.api.StoreType;

/**
 * An implementation of {@link FileStore} that stores and retrieves data from the local filesystem.
 * <p>
 * This class provides local file storage functionality as an alternative to S3-based storage.
 * It implements both {@link FileStore} and {@link ConnectionDataStore} interfaces.
 * <p>
 * Configuration is provided via DataStore parameters and environment variables:
 * <ul>
 *   <li>FSB_ROOT_PATH - Base path for storage (required)</li>
 *   <li>DataStore "root" parameter - Subdirectory path relative to base, or absolute path</li>
 * </ul>
 *
 * @see FileStore
 * @see ConnectionDataStore
 */
public class FileStoreLocal implements FileStore, ConnectionDataStore {

    private String basePath;
    private StoreType storeType;
    private static final String ROOT_PARAM = "root";

    public FileStoreLocal() {
        this.storeType = StoreType.FS;
    }

    @Override
    public ConnectionDataStore connect(DataStore ds) throws FailedToConnectError {
        String rootPath = System.getenv(EnvironmentVariables.FSB_ROOT_PATH);
        if (rootPath == null || rootPath.isEmpty()) {
            throw new FailedToConnectError(new IllegalArgumentException(
                "FSB_ROOT_PATH environment variable is required for filesystem storage"));
        }

        // Get the "root" parameter from DataStore params (subdirectory)
        String subPath = null;
        try {
            Optional<String> optParam = ds.getParameters().get(ROOT_PARAM);
            if (optParam.isPresent()) {
                subPath = optParam.get();
            }
        } catch (Exception e) {
            throw new FailedToConnectError(e);
        }

        // Build the full base path
        Path basePath = Paths.get(rootPath);
        if (subPath != null && !subPath.isEmpty()) {
            if (subPath.startsWith("/")) {
                // Absolute path
                basePath = Paths.get(subPath);
            } else {
                // Relative to rootPath
                basePath = basePath.resolve(subPath);
            }
        }
        this.basePath = basePath.toString();

        // Ensure directory exists
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new FailedToConnectError(new Exception("Failed to create directory: " + this.basePath, e));
        }

        return this;
    }

    @Override
    public Object rawSession() {
        return basePath;
    }

    @Override
    public GetObjectOutput get(String path) throws DataStoreException {
        Path fullPath = Paths.get(basePath).resolve(path);
        try {
            if (!Files.exists(fullPath)) {
                throw new DataStoreException("File not found: " + fullPath);
            }

            InputStream is = new FileInputStream(fullPath.toFile());
            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return new GetObjectOutput(is, contentType);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public PutObjectOutput put(InputStream data, String path) throws DataStoreException {
        Path fullPath = Paths.get(basePath).resolve(path);
        try {
            // Create parent directories if needed
            Files.createDirectories(fullPath.getParent());

            Files.copy(data, fullPath, StandardCopyOption.REPLACE_EXISTING);

            return new PutObjectOutput("", "");
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public void delete(String path) throws DataStoreException {
        Path fullPath = Paths.get(basePath).resolve(path);
        try {
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public void copy(FileStore destStore, String srcPath, String destPath) throws DataStoreException {
        Path fullSrcPath = Paths.get(basePath).resolve(srcPath);
        try (InputStream is = new FileInputStream(fullSrcPath.toFile())) {
            destStore.put(is, destPath);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

}
