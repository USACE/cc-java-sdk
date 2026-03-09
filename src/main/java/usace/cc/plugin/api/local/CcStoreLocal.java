package usace.cc.plugin.api.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;

import usace.cc.plugin.api.CcStore;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.EnvironmentVariables;
import usace.cc.plugin.api.GetObjectInput;
import usace.cc.plugin.api.Payload;
import usace.cc.plugin.api.PullObjectInput;
import usace.cc.plugin.api.PutObjectInput;
import usace.cc.plugin.api.StoreType;

/**
 * An implementation of {@link CcStore} that stores and retrieves data from the local filesystem.
 * <p>
 * This class provides local storage functionality as an alternative to S3-based storage.
 * <p>
 * Configuration is provided via environment variables:
 * <ul>
 *   <li>FSB_ROOT_PATH - Root path for storage</li>
 *   <li>CC_ROOT - Subdirectory under root path</li>
 *   <li>CC_MANIFEST_ID - Manifest identifier</li>
 *   <li>CC_PAYLOAD_ID - Payload identifier</li>
 * </ul>
 *
 * @see CcStore
 */
public class CcStoreLocal implements CcStore {

    private String rootPath;
    private String ccRoot;
    private String manifestId;
    private String payloadId;
    private StoreType storeType;

    public CcStoreLocal() {
        this.rootPath = System.getenv(EnvironmentVariables.FSB_ROOT_PATH);
        if (this.rootPath == null || this.rootPath.isEmpty()) {
            throw new IllegalArgumentException(
                "FSB_ROOT_PATH environment variable is required for filesystem storage");
        }

        this.ccRoot = System.getenv(EnvironmentVariables.CC_ROOT);
        this.manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        this.payloadId = System.getenv(EnvironmentVariables.CC_PAYLOAD_ID);
        this.storeType = StoreType.FS;

        // Ensure root directory exists
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public boolean handlesDataStoreType(StoreType storeType) {
        return this.storeType == storeType;
    }

    @Override
    public void putObject(PutObjectInput input) throws DataStoreException {
        Path destPath = buildPath(rootPath, ccRoot, manifestId,
            input.getFileName() + "." + input.getFileExtension());

        try {
            // Create parent directories if they don't exist
            Files.createDirectories(destPath.getParent());

            byte[] data;
            switch (input.getObjectState()) {
                case LOCAL_DISK:
                    Path sourcePath = Paths.get(input.getSourcePath());
                    if (!Files.exists(sourcePath)) {
                        throw new DataStoreException("Source file not found: " + sourcePath);
                    }
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    break;
                case MEMORY:
                    data = input.getData();
                    Files.write(destPath, data);
                    break;
                default:
                    throw new DataStoreException("Invalid object state");
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public void pullObject(PullObjectInput input) throws DataStoreException {
        Path sourcePath = buildPath(rootPath, ccRoot, manifestId,
            input.getFileName() + "." + input.getFileExtension());

        Path destPath = buildPath(input.getDestRootPath(),
            input.getFileName() + "." + input.getFileExtension());

        try {
            // Create parent directories if they don't exist
            Files.createDirectories(destPath.getParent());

            // Copy file from source to destination
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public byte[] getObject(GetObjectInput input) throws Exception {
        Path path = buildPath(rootPath, ccRoot, payloadId,
            input.getFileName() + "." + input.getFileExtension());

        return Files.readAllBytes(path);
    }

    @Override
    public Payload getPayload() throws Exception {
        Path path = buildPath(rootPath, ccRoot, payloadId, CcStore.PAYLOAD_FILE_NAME);

        byte[] data = Files.readAllBytes(path);
        return readJsonModelPayloadFromBytes(data);
    }

    private Path buildPath(String... parts) {
        Path path = null;
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (path == null) {
                    path = Paths.get(part);
                } else {
                    path = path.resolve(part);
                }
            }
        }
        return path;
    }

    private Payload readJsonModelPayloadFromBytes(byte[] bytes) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(bytes, Payload.class);
    }
}
