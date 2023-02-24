package usace.wat.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CcStoreS3 implements CcStore {
    String localRootPath;
    String remoteRootPath;
    String manifestId;
    StoreType storeType;
    AmazonS3 awsS3;
    AWSConfig config;
    @Override
    public String RootPath() {
        return remoteRootPath;
    }
    public CcStoreS3(){
        AWSConfig acfg = new AWSConfig();
        acfg.aws_access_key_id = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID);
        acfg.aws_secret_access_key_id = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY);
        acfg.aws_region = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_DEFAULT_REGION);
        acfg.aws_bucket = System.getenv(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_S3_BUCKET);
        acfg.aws_mock = false; //Boolean.parseBoolean(System.getenv("S3_MOCK"));//convert to boolean;
        //acfg.aws_endpoint = System.getenv("S3_ENDPOINT");
        //acfg.aws_disable_ssl = Boolean.parseBoolean(System.getenv("S3_DISABLE_SSL"));//convert to bool?
        //acfg.aws_force_path_style = Boolean.parseBoolean(System.getenv("S3_FORCE_PATH_STYLE"));//convert to bool
        config = acfg;
        System.out.println(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_DEFAULT_REGION+"::"+config.aws_region);
        System.out.println(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID+"::"+config.aws_access_key_id);
        System.out.println(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY+"::"+config.aws_secret_access_key_id);
        System.out.println(EnvironmentVariables.CC_PROFILE + "_" + EnvironmentVariables.AWS_S3_BUCKET+"::"+config.aws_bucket);
        Regions clientRegion = Regions.valueOf(config.aws_region.toUpperCase().replace("-", "_"));
        try {
            AmazonS3 s3Client = null;
            if(config.aws_mock){
                AWSCredentials credentials = new BasicAWSCredentials(config.aws_access_key_id, config.aws_secret_access_key_id);
                ClientConfiguration clientConfiguration = new ClientConfiguration();
                clientConfiguration.setSignerOverride("AWSS3V4SignerType");
                clientConfiguration.setProtocol(Protocol.HTTP);

                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(config.aws_endpoint, clientRegion.name()))
                    .withPathStyleAccessEnabled(config.aws_force_path_style)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            }else{
                AWSCredentials credentials = new BasicAWSCredentials(config.aws_access_key_id, config.aws_secret_access_key_id);
                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();                
            }
            awsS3 = s3Client;
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
        storeType = StoreType.S3;
        manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        localRootPath = Constants.LocalRootPath;
        remoteRootPath =  config.aws_bucket;// + Constants.RemoteRootPath;
    }
    @Override
    public boolean HandlesDataStoreType(StoreType storeType){
        return this.storeType == storeType;
    }
    @Override
    public boolean PutObject(PutObjectInput input) {
        String path = Constants.RemoteRootPath + "/" + manifestId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;
        switch(input.getObjectState()){
            case LocalDisk:
                //read from local
                File file = new File(path);
                data = new byte[(int) file.length()];
                try(FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                }
                catch(Exception e){
                    //@TODOprint?
                }
                UploadToS3(config.aws_bucket, path, data);
                break;
            case Memory:
                data = input.getData();
                UploadToS3(config.aws_bucket, path, data);
                break;
            default:
                return false;
        }
        
        return true;
    }
    @Override
    public boolean PullObject(PullObjectInput input) {
        String path = Constants.RemoteRootPath + "/" + manifestId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;
        String localPath = input.getDestRootPath() + "/" + input.getFileName() + "." + input.getFileExtension();
        try {
            //get the object from s3
            data = DownloadBytesFromS3(path);
            //create localpath writer
            InputStream stream = new ByteArrayInputStream(data);
            //write it.
            writeInputStreamToDisk(stream, localPath);
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    private void writeInputStreamToDisk(InputStream input, String outputDestination) throws IOException {
        String[] fileparts = outputDestination.split("/");
        String fileName = fileparts[fileparts.length-1];
        String directory = outputDestination.replace(fileName,"");
        File f = new File(directory);
        if(!f.exists()){
            f.mkdirs();
        }
        byte[] bytes = input.readAllBytes();
        OutputStream os = new FileOutputStream(new File(outputDestination));
        os.write(bytes);
    }
    @Override
    public byte[] GetObject(GetObjectInput input) throws RemoteException {
        String path = Constants.RemoteRootPath + "/" + manifestId + "/" + input.getFileName() + "." + input.getFileExtension();
        byte[] data;
        try {
            data = DownloadBytesFromS3(path);
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
        return data;
    }
    @Override
    public Payload GetPayload() throws RemoteException {
        String filepath = Constants.RemoteRootPath + "/" + manifestId + "/" + Constants.PayloadFileName;
        try{
            byte[] body = DownloadBytesFromS3(filepath);
            return ReadJsonModelPayloadFromBytes(body);
        } catch (Exception e){
            throw new RemoteException(e.toString());
        }
    }
    private byte[] DownloadBytesFromS3(String key) throws Exception{
        S3Object fullObject = null;
        System.out.println(key);
        System.out.println(remoteRootPath);
        boolean spaces = key.contains(" ");
        if(spaces){
            key = "\""+ key + "\""; 
        }
        try {
            fullObject = awsS3.getObject(new GetObjectRequest(remoteRootPath, key));
            System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
            return fullObject.getObjectContent().readAllBytes();
        }  catch (Exception e) {
            throw e;
        } finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                try {
                    fullObject.close();
                }  catch (Exception e) {
                    throw e;
                }
            }
        }
    }
    private Payload ReadJsonModelPayloadFromBytes(byte[] bytes) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(); // jackson databind
        try {
            return mapper.readValue(bytes, Payload.class);
        } catch (Exception e) {
            throw e;
        }
    }
    private void UploadToS3(String bucketName, String objectKey, byte[] fileBytes) {
        try {
            //File file = new File(objectPath);
            InputStream stream = new ByteArrayInputStream(fileBytes);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(fileBytes.length);
            PutObjectRequest putOb = new PutObjectRequest(bucketName, objectKey,stream, meta);
            PutObjectResult response = awsS3.putObject(putOb);
            System.out.println(response.getETag());
        } catch (SdkBaseException e) {
            System.out.println(e.getMessage());
        }
    }
}

