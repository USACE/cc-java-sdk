package usace.wat.plugin.utils;
 
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;


public class Loader {
    private Config _config = new Config();
    private AmazonS3 _client = null;
    public Loader(){
        //read environment variables
        //setting by default for now for testing.
        _config.AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
        _config.AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        _config.AWS_DEFAULT_REGION = "us_east_1";
        _config.AWS_S3_REGION = "us_east_1";
        _config.S3_MOCK = true;
        _config.S3_ENDPOINT = "http://host.docker.internal:9000";
        _config.S3_FORCE_PATH_STYLE = true;


        Regions clientRegion = Regions.valueOf(_config.AWS_DEFAULT_REGION.toUpperCase());
        try {
            AmazonS3 s3Client = null;
            if(_config.S3_MOCK){
                AWSCredentials credentials = new BasicAWSCredentials(_config.AWS_ACCESS_KEY_ID, _config.AWS_SECRET_ACCESS_KEY);
                ClientConfiguration clientConfiguration = new ClientConfiguration();
                clientConfiguration.setSignerOverride("AWSS3V4SignerType");

                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(_config.S3_ENDPOINT, clientRegion.name()))
                    .withPathStyleAccessEnabled(_config.S3_FORCE_PATH_STYLE)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            }else{
                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    //requires credentials to be set via env variables AWS_SECRET_KEY ...
                    .withCredentials(new ProfileCredentialsProvider())
                    .build();                
            }
            _client = s3Client;
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
        
    }
    public void UploadToS3(String bucketName, String objectKey, String objectPath) {
        try {
            File file = new File(objectPath);
            PutObjectRequest putOb = new PutObjectRequest(bucketName, objectKey, file);
            PutObjectResult response = _client.putObject(putOb);
            System.out.println(response.getETag());
        } catch (SdkBaseException e) {
            System.out.println(e.getMessage());
        }
    }
    public void DownloadFromS3(String bucketName, String key, String outputDestination){
        S3Object fullObject = null;
        try {
            // Get an object and print its contents.
            System.out.println("Downloading an object");
            fullObject = _client.getObject(new GetObjectRequest(bucketName, key));
            System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
            writeInputStreamToDisk(fullObject.getObjectContent(), outputDestination);
        } catch (IOException e) {
            
            e.printStackTrace();
        } finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                try {
                    fullObject.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static void writeInputStreamToDisk(InputStream input, String outputDestination) throws IOException {
        // Read the text input stream one line at a time and display each line.
        
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
}
