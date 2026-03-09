package usace.cc.plugin.api.cloud.aws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
//import software.amazon.nio.spi.s3.
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import usace.cc.plugin.api.ConnectionDataStore;
import usace.cc.plugin.api.DataStore;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.EnvironmentVariables;
import usace.cc.plugin.api.FileStore;
import usace.cc.plugin.api.GetObjectOutput;
import usace.cc.plugin.api.IOManager;
import usace.cc.plugin.api.IOManager.FileVisitor;
import usace.cc.plugin.api.PutObjectOutput;
import usace.cc.plugin.api.StoreType;

public class FileStoreS3 implements FileStore, ConnectionDataStore {
    String bucket;
    String postFix;
    StoreType storeType;
    S3Client awsS3;
    AWSConfig config;
    private static String S3ROOT = "root";

    public static class S3FileObject implements IOManager.FileObject{
        private final S3Object obj;
        private final FileStoreS3 fs;
        private final boolean useAbsolutePath;

        public S3FileObject(FileStoreS3 fs, S3Object obj, boolean useAbsolutePath){
            this.fs=fs;
            this.obj=obj;
            this.useAbsolutePath=useAbsolutePath;
        }

        @Override
        public String name() {
            return this.obj.key();
        }

        @Override
        public GetObjectOutput get() throws DataStoreException{
            return this.fs.get(this.obj.key(),useAbsolutePath);
        }
    }

    public FileStoreS3(){}

    @Override
    public void copy(FileStore destStore, String srcPath, String destPath) throws DataStoreException{
        byte[] data;
        try {
            data = getObject(srcPath);
            ByteArrayInputStream bias = new ByteArrayInputStream(data);
            destStore.put(bias, destPath);
        } catch (RemoteException e) {
           throw new DataStoreException(e);
        }
    }

    /**
     * Retrieves an input stream for the specified file in the S3 storage.
     *
     * @param path The path of the file to retrieve.
     * @return An {@link InputStream} for the file, or {@code null} if an error occurs.
     */
    @Override
    public GetObjectOutput get(String path) throws DataStoreException{
        return this.get(path,false);
    }

    private GetObjectOutput get(String path, boolean useAbsolutePath) throws DataStoreException{
        if (!useAbsolutePath){
            path=postFix + "/"+ path;
        }
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .build();
            
        try {
            ResponseInputStream<GetObjectResponse> responseIs = awsS3.getObject(request, ResponseTransformer.toInputStream());
            var response = responseIs.response();
            return new GetObjectOutput(responseIs, response.contentType());
        } catch (S3Exception e) {
           throw new DataStoreException(e);
        } 

    }

    /**
     * Uploads data to the specified path in the S3 storage.
     *
     * @param data The input stream containing the data to upload.
     * @param path The path where the data should be stored in the S3 storage.
     * @return {@code true} if the upload operation is successful, otherwise {@code false}.
     */
    @Override
    public PutObjectOutput put(InputStream data, String path) throws DataStoreException{
        try {
            byte[] bytes = data.readAllBytes();
            return uploadToS3(config.aws_bucket, postFix + "/" + path, bytes);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Deletes a file from the specified path in the S3 storage.
     *
     * @param path The path of the file to delete.
     * @return {@code true} if the deletion operation is successful, otherwise {@code false}.
     */
    @Override
    public void delete(String path) throws DataStoreException {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(config.aws_bucket)
            .key(postFix + "/" + path)
            .build();
            
        try{
            awsS3.deleteObject(request);
        } catch (S3Exception e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the underlying AWS S3 client object.
     *
     * @return The {@link S3Client} client object.
     */
    @Override
    public Object rawSession(){
        return awsS3;
    }
    
    /**
     * Establishes a connection to the S3 storage using provided configuration data.
     *
     * @param ds The {@link DataStore} containing the necessary configuration parameters.
     * @return This instance of {@link FileStoreS3} after establishing the connection.
     * @throws FailedToConnectError If the connection cannot be established.
     */
    @Override
    public ConnectionDataStore connect(DataStore ds) throws FailedToConnectError{
        config = new AWSConfig();
        config.aws_access_key_id = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_ACCESS_KEY_ID);
        config.aws_secret_access_key_id = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_SECRET_ACCESS_KEY);
        config.aws_region = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_DEFAULT_REGION);
        config.aws_bucket = System.getenv(ds.getDsProfile() + "_" + EnvironmentVariables.AWS_S3_BUCKET);
        config.aws_endpoint = System.getenv(ds.getDsProfile() + "_"+ EnvironmentVariables.AWS_ENDPOINT);
        
        Region clientRegion = Region.of(config.aws_region);
        try {
            var clientBuilder = AmazonS3ClientBuilder.standard();
            if (config.aws_access_key_id != null && !config.aws_access_key_id.isEmpty()) {
                AWSCredentials credentials = new BasicAWSCredentials(config.aws_access_key_id, config.aws_secret_access_key_id);
                clientBuilder.withCredentials(new AWSStaticCredentialsProvider(credentials));
            }

            if (config.aws_endpoint != null && !config.aws_endpoint.isEmpty()) {
                clientBuilder
                .endpointOverride(URI.create(config.aws_endpoint))
                .forcePathStyle(true);
            }

            awsS3 = clientBuilder.build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        storeType = StoreType.S3;

        String tmpRoot="";
        
        try {
            Optional<String> optParam = ds.getParameters().get(FileStoreS3.S3ROOT);
            if (optParam.isPresent()){
                tmpRoot = optParam.get();
            }
        } catch (Exception e) {
           throw new FailedToConnectError(e);
        }
        if (tmpRoot == ""){
            System.out.print("Missing S3 Root Paramter. Cannot create the store.");  //@TODO...shouldn't this be throwing an error?
        }
        this.bucket = config.aws_bucket;
        tmpRoot = tmpRoot.replaceFirst("^/+", "");
        this.postFix = tmpRoot;
        return this;
    }

    private byte[] getObject(String path) throws RemoteException {
        byte[] data;
        try {
            data = downloadBytesFromS3(path);
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
        return data;
    }

    private byte[] downloadBytesFromS3(String key) throws Exception{
        key = postFix + "/" + key;
        //System.out.println(key);
        //System.out.println(bucket);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
                

             ResponseBytes<GetObjectResponse> responseBytes = awsS3.getObject(request, ResponseTransformer.toBytes());
            return responseBytes.asByteArray();
        } catch (Exception e) {
            throw e;
        }
    }

    private PutObjectOutput uploadToS3(String bucketName, String objectKey, byte[] fileBytes) throws DataStoreException {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
                
            RequestBody body = RequestBody.fromBytes(fileBytes);
            PutObjectResponse response = awsS3.putObject(request, body);
            //System.out.println(response.eTag());
            return new PutObjectOutput(response.eTag(), response.eTag()); // MD5 not available in v2, using ETag
        } catch (S3Exception e) {
            throw new DataStoreException(e);
        }
    }

    private void walkImpl(String absolutePath, FileVisitor visitor){
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket("project-data")
                    .prefix(absolutePath)
                    .delimiter("/") 
                    .build();

        ListObjectsV2Iterable paginator = awsS3.listObjectsV2Paginator(request);
        for (var response : paginator) {
            for (S3Object object : response.contents()) {
                var fo = new S3FileObject(this,object,true);
                visitor.visit(fo);
            }
            
            for (CommonPrefix commonPrefix : response.commonPrefixes()) {
                //recursively walk the common prefixes
                walkImpl(commonPrefix.prefix(),visitor);
            }
        }

    }

    @Override
    public void walk(String path, FileVisitor visitor) {
        if (path != null && path.startsWith("/")) {
            path=path.substring(1);
        }
        path=String.format("%s/%s",postFix,path);
        walkImpl(path, visitor);
    }
}