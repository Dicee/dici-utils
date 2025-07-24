package com.dici.aws.s3;

import com.dici.aws.exception.AwsDependencyException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface S3Client {
    HeadObjectResponse headObject(String bucket, String key);

    default boolean doesObjectExist(String bucket, String key) {
        try {
            headObject(bucket, key);
            return true;
        } catch (AwsDependencyException e) {
            if (e.getCause() instanceof AwsServiceException se && se.statusCode() == 404) return false;
            throw e;
        }
    }

    /// Note: the AWS SDK advises to consume and close the stream as quickly as possible to avoid
    ///       opening too many connections and starving other threads trying to open new connections.
    InputStream getObjectStream(String bucket, String key);

    String getObjectAsString(String bucket, String key);

    void getObjectToFile(String bucket, String key, Path destination);

    /// Eagerly lists all the objects in a given bucket under a certain prefix. May run multiple batch requests.
    List<S3Object> listObjects(String bucket, String prefix);

    /// Lazily lists all the objects in a given bucket under a certain prefix. May run multiple batch requests.
    Stream<S3Object> streamListObjects(String bucket, String prefix);

    PutObjectResponse putObject(String bucket, String key, Path source, PutOptions putOptions);

    PutObjectResponse putObject(String bucket, String key, String content, PutOptions putOptions);

    void deleteObject(String bucket, String key);

    CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest);
}
