package usace.cc.plugin.api.cloud.aws;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
//import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder;

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
 * An implementation of {@link CcStore} that stores and retrieves data from Amazon S3.
 * <p>
 * This class provides cloud-based storage functionality for internal SDK usage using the AWS S3 service.
 * It is intended to serve as a backend implementation of the {@code CcStore} interface.
 * <p>
 * Dependencies such as AWS credentials, region, and bucket configuration are expected
 * to be provided externally, via environment variables, configuration files,
 * or dependency injection.
 *
 * @see CcStore
 */
public class CcStoreS3 implements CcStore {
    String localRootPath;
    String bucket;
    String root;
    String manifestId;
    String payloadId;
    StoreType storeType;
    S3Client awsS3;
    AWSConfig config;

    public CcStoreS3() {
        AWSConfig acfg = new AWSConfig();
        acfg.aws_access_key_id = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID);
        acfg.aws_secret_access_key_id = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY);
        acfg.aws_region = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_DEFAULT_REGION);
        acfg.aws_bucket = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_S3_BUCKET);
        acfg.aws_endpoint = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_ENDPOINT);        
        config = acfg;

        Region clientRegion = Region.of(config.aws_region);
 
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(clientRegion)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.aws_access_key_id, config.aws_secret_access_key_id)
                ));

        if (!(config.aws_endpoint == null || config.aws_endpoint.equals(""))) {
            System.out.println(String.format("Using alt endpoint: %s", config.aws_endpoint));
            config.aws_force_path_style = true;
            config.aws_disable_ssl = true;
            
            s3ClientBuilder
                .endpointOverride(URI.create(config.aws_endpoint))
                .forcePathStyle(true);
        }

        awsS3 = s3ClientBuilder.build();
        
        storeType = StoreType.S3;
        manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        payloadId = System.getenv(EnvironmentVariables.CC_PAYLOAD_ID);
        bucket = config.aws_bucket;
        root = System.getenv(EnvironmentVariables.CC_ROOT);
    }
    
    @Override
    public String rootPath() {
        return bucket;
    }

    @Override
    public boolean handlesDataStoreType(StoreType storeType){
        return this.storeType == storeType;
    }

    @Override
    public void putObject(PutObjectInput input) throws DataStoreException {
        String path = root + "/" + manifestId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;
        switch(input.getObjectState()){
            case LOCAL_DISK:
                //read from local
                File file = new File(path);
                data = new byte[(int) file.length()];
                try(FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                }
                catch(Exception e){
                    //@TODOprint?
                }
                uploadToS3(config.aws_bucket, path, data);
                break;
            case MEMORY:
                data = input.getData();
                uploadToS3(config.aws_bucket, path, data);
                break;
            default:
                throw new DataStoreException("Invalid object state");
        }
    }

    @Override
    public void pullObject(PullObjectInput input) throws DataStoreException {
        String path = root + "/" + manifestId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;
        String localPath = input.getDestRootPath() + "/" + input.getFileName() + "." + input.getFileExtension();
        try {
            //get the object from s3
            data = downloadBytesFromS3(path);
            //create localpath writer
            InputStream stream = new ByteArrayInputStream(data);
            //write it.
            writeInputStreamToDisk(stream, localPath);
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public byte[] getObject(GetObjectInput input) throws Exception {
        String path = root + "/" + payloadId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;

            data = downloadBytesFromS3(path);
       
        return data;
    }

    @Override
    public Payload getPayload() throws S3Exception {
        String filepath = root + "/" + payloadId + "/" + CcStore.PAYLOAD_FILE_NAME;
        try{
            byte[] body = downloadBytesFromS3(filepath);
            return readJsonModelPayloadFromBytes(body);
        } catch (Exception e){
            throw S3Exception.builder().message(e.toString()).build();
        }
    }

    private void writeInputStreamToDisk(InputStream input, String outputDestination) throws IOException {
        String directory = new File(outputDestination).getParent();
        File f = new File(directory);
        if(!f.exists()){
            f.mkdirs();
        }
        byte[] bytes = input.readAllBytes();
        try (OutputStream os = new FileOutputStream(new File(outputDestination))) {
            os.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

    private byte[] downloadBytesFromS3(String key) throws IOException {
        boolean spaces = key.contains(" ");

        if(spaces){
            key = "\""+ key + "\""; 
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = awsS3.getObject(getObjectRequest, ResponseTransformer.toBytes());
        var response = responseBytes.response();

        System.out.println("Content-Type: " + response.contentType());
        return responseBytes.asByteArray();
    }

    private Payload readJsonModelPayloadFromBytes(byte[] bytes) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(); // jackson databind
        try {
            return mapper.readValue(bytes, Payload.class);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private void uploadToS3(String bucketName, String objectKey, byte[] fileBytes) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        RequestBody requestBody = RequestBody.fromBytes(fileBytes);
        PutObjectResponse response = awsS3.putObject(putObjectRequest, requestBody);
        System.out.println(response.eTag()); //@use logger here?
    }
}