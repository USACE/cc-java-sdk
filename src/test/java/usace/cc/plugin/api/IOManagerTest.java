package usace.cc.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.s3.S3Client;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.InvalidDataSourceException;
import usace.cc.plugin.api.IOManager.InvalidDataStoreException;

public class IOManagerTest {

    private PluginManager pm;
    private Payload payload;

    @BeforeEach
    void setUp() throws InvalidDataStoreException {
        pm = PluginManager.getInstance();
        payload = pm.getPayload();            
    }

    @Test
    public void testGetStore() {
        Optional<DataStore> storeOpt = payload.getStore("S3STORE");
        if (storeOpt.isPresent()){
            var store = storeOpt.get();
            if (!(store.getRawSession() instanceof S3Client)){
                Assertions.fail("Session is not an S3 instance");    
            }
        } else {
            Assertions.fail("Unable to load S3STORE");
        }
    }

    @Test
    public void testGetDataSource() throws InvalidDataSourceException {
        var inputName = "image1";
        GetDataSourceInput gdsi = new GetDataSourceInput(inputName, DataSourceIOType.INPUT);
        Optional<DataSource> dataSourceOpt = payload.getDataSource(gdsi);
        assertTrue(dataSourceOpt.isPresent());
        var dataSource = dataSourceOpt.get();
        assertEquals(dataSource.getName(), inputName);
    }

    @Test
    public void testGetDataSourceInput() throws InvalidDataSourceException {
        var inputName = "image1";
        Optional<DataSource> dataSourceOpt = payload.getInputDataSource(inputName);
        assertTrue(dataSourceOpt.isPresent());
        var dataSource = dataSourceOpt.get();
        assertEquals(dataSource.getName(), inputName);
    }

    @Test
    public void testGetDataSourceOutput() throws InvalidDataSourceException {
        var inputName = "test-out1";
        Optional<DataSource> dataSourceOpt = payload.getOutputDataSource(inputName);
        assertTrue(dataSourceOpt.isPresent());
        var dataSource = dataSourceOpt.get();
        assertEquals(dataSource.getName(), inputName);
    }

    @Test
    public void testGetInputStream() throws IOException, InvalidDataSourceException, DataStoreException {
        var inputName = "text1";
        var expectedResult="hello world";
        Optional<DataSource> dataSourceOpt = payload.getInputDataSource(inputName);
        assertTrue(dataSourceOpt.isPresent());
        var dataSource = dataSourceOpt.get();
        InputStream is = payload.getInputStream(dataSource, "default");
        var result = isToString(is);
        assertEquals(result,expectedResult);
    }

    @Test
    public void testCopyFileToLocal() throws IOException, InvalidDataSourceException, DataStoreException {
        String localPath = "/data/testfile";
        int fileSize = 146050;
        payload.copyFileToLocal("image1", "default", localPath);
        File file = new File(localPath);
        assertTrue(file.exists());
        Path path = Paths.get(localPath);
        var bytes = Files.readAllBytes(path);
        assertEquals(bytes.length, fileSize);
    }

    @Test
    public void testPut() throws IOException, InvalidDataSourceException, DataStoreException, InvalidDataStoreException {
        byte[] data = "mockData".getBytes();
        PutObjectOutput putObjectOutput = payload.put(data, "test-out2", "default", null);
        var transferredData = payload.get("test-in1", "default", null);
        var md5hash = md5(transferredData);
        //@TODO review filecode vs content hash in the PutObjectOutput.
        //byte[] decodedBytes = Base64.getDecoder().decode(putObjectOutput.getHash());
        //String contentHash = new String(decodedBytes);
        assertEquals(putObjectOutput.getFilecode(), md5hash);
        //assertEquals(contentHash, md5hash);
    }

    @Test
    public void testCopyFileToRemote() throws IOException, InvalidDataSourceException, DataStoreException, InvalidDataStoreException {
        String localPath = "/data/testfile";
        int fileSize = 146050;
        payload.copyFileToRemote("test-out1", "default", localPath);
        var response = payload.get("test4","default",null);
        assertEquals(response.length,fileSize);
    }

    @Test
    public void testGetStoreSession() throws InvalidDataStoreException {
        Optional<FileStore> session = payload.getStoreSession("S3STORE");
        assertTrue(session.isPresent());
    }


    private static String isToString(InputStream inputStream) throws IOException {
        var bytes = inputStream.readAllBytes(); 
        return new String(bytes);
    }

    public static String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);

            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }


    
}
