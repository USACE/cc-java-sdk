package usace.wat.plugin;
 
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
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import usace.wat.plugin.Message.Level;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;

public final class Utilities {
    private static Config _config;
    private static AmazonS3 _client = null;
    private static Boolean _hasInitalized = false;
    private Utilities(){
        InitalizeFromPath("config.json");
    }
    private Utilities(String jsonFilePath){
        InitalizeFromPath(jsonFilePath);
    }
    private Utilities(Config config){
        Initalize(config);
    }
    private static void InitalizeFromPath(String path){
        //read from json to fill a configuration.
        Config cfg = new Config();
        File file = new File(path);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            cfg = mapper.readValue(file, Config.class);
        } catch (JsonParseException e) {
            e.printStackTrace();//bad form
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Initalize(cfg);
    }
    private static void Initalize(Config config){
        _config = config;
        AWSConfig awsconfig = _config.PrimaryConfig();
        Regions clientRegion = Regions.valueOf(awsconfig.aws_region.toUpperCase());
        try {
            AmazonS3 s3Client = null;
            if(awsconfig.aws_mock){
                AWSCredentials credentials = new BasicAWSCredentials(awsconfig.aws_access_key_id, awsconfig.aws_secret_access_key_id);
                ClientConfiguration clientConfiguration = new ClientConfiguration();
                clientConfiguration.setSignerOverride("AWSS3V4SignerType");

                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsconfig.aws_endpoint, clientRegion.name()))
                    .withPathStyleAccessEnabled(awsconfig.aws_force_path_style)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            }else{
                AWSCredentials credentials = new BasicAWSCredentials(awsconfig.aws_access_key_id, awsconfig.aws_secret_access_key_id);
                s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
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
        _hasInitalized = true;        
    }
    private static void UploadToS3(String bucketName, String objectKey, String objectPath) {
        try {
            File file = new File(objectPath);
            PutObjectRequest putOb = new PutObjectRequest(bucketName, objectKey, file);
            PutObjectResult response = _client.putObject(putOb);
            System.out.println(response.getETag());
        } catch (SdkBaseException e) {
            System.out.println(e.getMessage());
        }
    }
    private static void DownloadFromS3(String bucketName, String key, String outputDestination){
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
    private static ModelPayload ReadYamlFromBytes(byte[] bytes) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            return mapper.readValue(bytes, ModelPayload.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelPayload();
    }
    public static ModelPayload LoadPayload(String filepath){
        //use primary s3 bucket to find the payload.
        Message message = Message.BuildMessage()
            .withMessage("reading payload at path: " + filepath)
            .withErrorLevel(Level.INFO)
            .fromSender("Plugin Services")
            .build();
        Log(message);
        ModelPayload payload = new ModelPayload();
        AWSConfig config = _config.PrimaryConfig();
        if (config == null) {
            return payload;
        }
        /*
        fs, err := getStore(config.AWS_BUCKET)
        if err != nil {
            return payload, err
        }
        data, err := fs.GetObject(filepath)
        if err != nil {
            return payload, err
        }
    
        body, err := ioutil.ReadAll(data)
        if err != nil {
            return payload, err
        }
    */
    return ReadYamlFromBytes(null);
        /*
        if err != nil {
            Log(Message{
                Message: fmt.Sprintf("error reading:%v", filepath),
                Level:   ERROR,
                Sender:  "Plugin Services",
            })
        }*/
        
    }
    public static void Log(Message message){
        System.out.println(message);
    }
}