package usace.wat.plugin;
 
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import usace.wat.plugin.Message.Level;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class Utilities {
    private Config _config;
    private Map<String,AmazonS3> _clients;
    private Boolean _hasInitalized = false;
    private Level _logLevel = Level.INFO;
    private static Utilities Instance = new Utilities();
    private Config getConfig(){
        return _config;
    }
    private AmazonS3 getClient(String bucketname){
        return _clients.get(bucketname);
    }
    private Boolean getHasInitalized(){
        return _hasInitalized;
    } 
    private Level getLogLevel(){
        return _logLevel;
    }
    private void setConfig(Config cfg){
        _config = cfg;
    }
    private void setClient(String bucketname, AmazonS3 client){
        _clients.put(bucketname, client);
    }
    private void setClientMap(Map<String,AmazonS3> s3){
        _clients = s3;
    }
    private void setHasInitalized(Boolean value){
        _hasInitalized = value;
    } 
    private void setInternalLogLevel(Level level){
        _logLevel = level;
    }
    public static void setLogLevel(Level level){
        Instance.setInternalLogLevel(level);
    }
    private Utilities(){
        //InitalizeFromPath("config.json");
        _clients = new HashMap<>();
    }
    public static void Initalize(){
        InitalizeFromPath("config.json");
    }
    public static void InitalizeFromPath(String path){
        //read from json to fill a configuration.
        Config cfg = new Config();
        File file = new File(path);

        final ObjectMapper mapper = new ObjectMapper();
        try {
            cfg = mapper.readValue(file, Config.class);
        } catch (Exception e) {
            Message message = Message.BuildMessage()
            .withMessage("Error Parsing Configuration Contents: at path " + path + " with error " + e.getMessage())
            .withErrorLevel(Level.ERROR)
            .fromSender("Plugin Services")
            .build();
            Log(message);
        }
        Initalize(cfg);
    }
    public static void InitalizeFromEnv(){
        Config cfg = new Config();
        
        Initalize(cfg);
    }
    public static void Initalize(Config config){
        Instance.setConfig(config);
        for (AWSConfig awsConfig : config.aws_configs) {
            AddS3Bucket(awsConfig);
        }
        Instance.setHasInitalized(true);        
    }
    private static void AddS3Bucket(AWSConfig awsconfig) {
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
            Instance.setClient(awsconfig.aws_bucket, s3Client);
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
    private static void UploadToS3(String bucketName, String objectKey, byte[] fileBytes) {
        try {
            //File file = new File(objectPath);
            InputStream stream = new ByteArrayInputStream(fileBytes);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(fileBytes.length);
            PutObjectRequest putOb = new PutObjectRequest(bucketName, objectKey,stream, meta);
            PutObjectResult response = Instance.getClient(bucketName).putObject(putOb);
            System.out.println(response.getETag());
        } catch (SdkBaseException e) {
            System.out.println(e.getMessage());
        }
    }
    private static byte[] DownloadBytesFromS3(String bucketName, String key){
        S3Object fullObject = null;
        try {
            // Get an object and print its contents.
            Message message = Message.BuildMessage()
            .withMessage("Downloading from S3: " + bucketName + "/" + key)
            .withErrorLevel(Level.INFO)
            .fromSender("Plugin Services")
            .build();
            Log(message);
            fullObject = Instance.getClient(bucketName).getObject(new GetObjectRequest(bucketName, key));
            System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
            return fullObject.getObjectContent().readAllBytes();
        }  catch (Exception e) {
            Message message = Message.BuildMessage()
            .withMessage("Error Downloading from S3: " + e.getMessage())
            .withErrorLevel(Level.ERROR)
            .fromSender("Plugin Services")
            .build();
            Log(message);
        } finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                try {
                    fullObject.close();
                }  catch (Exception e) {
                    Message message = Message.BuildMessage()
                    .withMessage("Error Closing S3 object: " + e.getMessage())
                    .withErrorLevel(Level.ERROR)
                    .fromSender("Plugin Services")
                    .build();
                    Log(message);
                }
            }
        }
        return null;
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
    private static ModelPayload ReadYamlModelPayloadFromBytes(byte[] bytes) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            return mapper.readValue(bytes, ModelPayload.class);
        } catch (Exception e) {
            Message message = Message.BuildMessage()
            .withMessage("Error Parsing Payload Contents: " + e.getMessage())
            .withErrorLevel(Level.ERROR)
            .fromSender("Plugin Services")
            .build();
            Log(message);
        }
        return new ModelPayload();
    }
    public static ModelPayload LoadPayload(String filepath){
        //use primary s3 bucket to find the payload.
        if (!Instance.getHasInitalized()){
            InitalizeFromPath("config.json");
        }
        Message message = Message.BuildMessage()
            .withMessage("reading payload at path: " + filepath)
            .withErrorLevel(Level.INFO)
            .fromSender("Plugin Services")
            .build();
        Log(message);
        ModelPayload payload = new ModelPayload();
        if (Instance.getConfig().aws_configs.length==0){
            Message message2 = Message.BuildMessage()
                .withMessage("Configuration contains no AWS Configurations")
                .withErrorLevel(Level.ERROR)
                .fromSender("Plugin Services")
                .build();
            Log(message2);
            return null;//not sure - probably throw an exception instead.
        }
        AWSConfig config = Instance.getConfig().PrimaryConfig();
        if (config == null) {
            return payload;
        }
        byte[] body = DownloadBytesFromS3(config.aws_bucket, filepath);
        return ReadYamlModelPayloadFromBytes(body);
    }
    public static void Log(Message message){
        if(message.getLevel().compareTo(Instance.getLogLevel())>=0){//test.
            System.out.println(message.toString());
        }
    }
    public static byte[] DownloadObject(ResourceInfo info){
        switch(info.getStore()){
            case S3:
                return DownloadBytesFromS3(info.getRoot(), info.getPath());
            case LOCAL:
                return null;
            default:
            return null;
        }
    }
    public static void UploadFile(ResourceInfo info, byte[] fileBytes){
        switch(info.getStore()){
            case S3:
                UploadToS3(info.getRoot(), info.getPath(), fileBytes);
                break;
            case LOCAL:
                InputStream stream = new ByteArrayInputStream(fileBytes);
                try {
                    writeInputStreamToDisk(stream, info.getRoot() + File.pathSeparator + info.getPath());
                } catch (IOException e) {
                    Message message = Message.BuildMessage()
                    .withMessage("Error Uploading local file: " + info.getPath() + " " + e.getMessage())
                    .withErrorLevel(Level.ERROR)
                    .fromSender("Plugin Services")
                    .build();
                Log(message);
                } 
            break;
            default:
            Message message = Message.BuildMessage()
                .withMessage("Error Uploading local file to " + info.getStore())
                .withErrorLevel(Level.ERROR)
                .fromSender("Plugin Services")
                .build();
            Log(message);
            break;
        }
    }
    public static EventConfiguration LoadEventConfiguration(ResourceInfo resourceInfo) {
        Message message = Message.BuildMessage()
            .withMessage("reading event configuration at path: " + resourceInfo.getPath())
            .withErrorLevel(Level.INFO)
            .fromSender("Plugin Services")
            .build();
        Log(message);
        if (!Instance.getHasInitalized()){
            InitalizeFromPath("config.json");
        }
        byte[] body = DownloadBytesFromS3(resourceInfo.getRoot(), resourceInfo.getPath());
        final ObjectMapper mapper = new ObjectMapper(); // jackson databind json parsing
        try {
            return mapper.readValue(body, EventConfiguration.class);
        } catch (Exception e) {
            Message message2 = Message.BuildMessage()
            .withMessage("Error Parsing Event Configuration Contents: " + e.getMessage())
            .withErrorLevel(Level.ERROR)
            .fromSender("Plugin Services")
            .build();
            Log(message2);
        }
        return new EventConfiguration();
    }
    public static Boolean CopyPayloadInputsLocally(ModelPayload payload, String localroot){
        for (ResourcedFileData input: payload.getInputs()) {
            byte[] body = DownloadObject(input.getResourceInfo());
            InputStream stream = new ByteArrayInputStream(body);
            try {
                writeInputStreamToDisk(stream, localroot + input.getResourceInfo().getPath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //log an error message.
                e.printStackTrace();
                return false;
            }
            for(ResourcedInternalPathData internalInput : input.getInternalPaths()) {
                byte[] internalBody = DownloadObject(input.getResourceInfo());
                InputStream internalStream = new ByteArrayInputStream(internalBody);
                try {
                    writeInputStreamToDisk(internalStream, localroot + internalInput.getResourceInfo().getPath());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    //log an error message.
                    e.printStackTrace();
                    return false;
                } 
            }
        }
        return true;
    }

}