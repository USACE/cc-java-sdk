package usace.wat.plugin;

import java.rmi.RemoteException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
        acfg.aws_access_key_id = System.getenv(EnvironmentVariables.AWS_ACCESS_KEY_ID);
        acfg.aws_secret_access_key_id = System.getenv(EnvironmentVariables.AWS_SECRET_ACCESS_KEY);
        acfg.aws_region = System.getenv(EnvironmentVariables.AWS_DEFAULT_REGION);
        acfg.aws_bucket = System.getenv(EnvironmentVariables.AWS_S3_BUCKET);
        acfg.aws_mock = Boolean.parseBoolean(System.getenv("S3_MOCK"));//convert to boolean;
        acfg.aws_endpoint = System.getenv("S3_ENDPOINT");
        acfg.aws_disable_ssl = Boolean.parseBoolean(System.getenv("S3_DISABLE_SSL"));//convert to bool?
        acfg.aws_force_path_style = Boolean.parseBoolean(System.getenv("S3_FORCE_PATH_STYLE"));//convert to bool
        config = acfg;
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
        remoteRootPath = Constants.RemoteRootPath;
    }
    @Override
    public boolean HandlesDataStoreType(StoreType storeType){
        return this.storeType == storeType;
    }
    @Override
    public boolean PutObject(PutObjectInput input) {
        return false;
    }
    @Override
    public boolean PullObject(PullObjectInput input) {
        return false;
    }
    @Override
    public byte[] GetObject(GetObjectInput input) throws RemoteException {
        return null;
    }
    @Override
    public Payload GetPayload() throws RemoteException {
        String filepath = remoteRootPath + "/" + manifestId + "/" + Constants.PayloadFileName;
        try{
            byte[] body = DownloadBytesFromS3(filepath);
            return ReadYamlModelPayloadFromBytes(body);
        } catch (Exception e){
            throw new RemoteException(e.toString());
        }
        
        
    }
    private byte[] DownloadBytesFromS3(String key) throws Exception{
        S3Object fullObject = null;
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
    private Payload ReadYamlModelPayloadFromBytes(byte[] bytes) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            return mapper.readValue(bytes, Payload.class);
        } catch (Exception e) {
            throw e;
        }
    }

}

