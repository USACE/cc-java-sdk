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
import com.amazonaws.Protocol;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import usace.wat.plugin.Error.ErrorLevel;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class PluginManager {
    private CcStore cs;
    private String _manifestId;
    private Payload _payload;
    private Map<String,AmazonS3> _clients;
    private Boolean _hasInitalized = false;
    private Logger _logger;
    private AmazonS3 getClient(String bucketname){
        return _clients.get(bucketname);
    }
    private Boolean getHasInitalized(){
        return _hasInitalized;
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
    private void setInternalLogLevel(ErrorLevel level){
        _logger.setErrorLevel(level);
    }
    public PluginManager(){
        String sender = System.getenv(EnvironmentVariables.CC_PLUGIN_DEFINITION);
        _logger = new Logger(sender, ErrorLevel.WARN);
        _manifestId = System.getenv(EnvironmentVariables.CC_MANIFEST_ID);
        cs = new CcStoreS3();

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
                String path = localroot + input.getFileName();
                Message message = Message.BuildMessage()
                .withMessage("writing locally: " + path)
                .withErrorLevel(Level.INFO)
                .fromSender("Plugin Services")
                .build();
                Log(message);
                writeInputStreamToDisk(stream, path);//input.getResourceInfo().getPath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //log an error message.
                e.printStackTrace();
                return false;
            }
            if (input.getInternalPaths()!=null){
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

        }
        return true;
    }

}