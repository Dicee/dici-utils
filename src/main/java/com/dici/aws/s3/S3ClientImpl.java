package com.dici.aws.s3;

import com.dici.aws.exception.AwsErrorHandling;
import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dici.time.TimeUtils.logFriendlyDuration;

@Log4j2
@RequiredArgsConstructor
public class S3ClientImpl implements S3Client {
    private final software.amazon.awssdk.services.s3.S3Client s3;

    @Override
    public HeadObjectResponse headObject(String bucket, String key) {
        return getFromS3(bucket, key, (b, k) -> s3.headObject(builder -> builder.bucket(b).key(k)), "object metadata");
    }

    @Override
    public InputStream getObjectStream(String bucket, String key) {
        return getFromS3(bucket, key, (b, k) -> s3.getObject(builder -> builder.bucket(b).key(k)), "object stream");
    }

    @Override
    public String getObjectAsString(String bucket, String key) {
        return getFromS3(bucket, key, (b, k) -> s3.getObjectAsBytes(builder -> builder.bucket(b).key(k)).asUtf8String(), "object");
    }

    @Override
    public void getObjectToFile(String bucket, String key, Path destination) {
        try (InputStream inputStream = getObjectStream(bucket, key)) {
            Files.copy(inputStream, destination);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<S3Object> listObjects(String bucket, String prefix) {
        return streamListObjects(bucket, prefix).toList();
    }

    @Override
    public Stream<S3Object> streamListObjects(String bucket, String prefix) {
        return AwsErrorHandling.decorate(
                () -> "Failed to list bucket %s with prefix %s".formatted(bucket, prefix),
                () -> s3.listObjectsV2Paginator(builder -> builder.bucket(bucket).prefix(prefix)).contents().stream()
        );
    }

    @Override
    public PutObjectResponse putObject(String bucket, String key, Path source, PutOptions options) {
        return putObjectInternal(bucket, key, RequestBody.fromFile(source), options,
                () -> "Failed to put file %s in S3 for %s/%s".formatted(source.toAbsolutePath(), bucket, key));
    }

    @Override
    public PutObjectResponse putObject(String bucket, String key, String content, PutOptions options) {
        return putObjectInternal(bucket, key, RequestBody.fromString(content), options,
                () -> "Failed to put object in S3 for %s/%s".formatted(bucket, key));
    }

    private PutObjectResponse putObjectInternal(String bucket, String key, RequestBody body, PutOptions options, Supplier<String> msgSupplier) {
        return AwsErrorHandling.decorate(msgSupplier, () -> {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .metadata(options.getMetadata())
                    .build();

            Stopwatch stopwatch = Stopwatch.createStarted();
            PutObjectResponse response = s3.putObject(request, body);

            Duration duration = stopwatch.elapsed();
            double bitRate = body.optionalContentLength().get() / 1024.0 / duration.toSeconds();
            log.info("Done uploading %s to bucket %s in %s (%.2f kB/s)".formatted(request.key(), request.bucket(), logFriendlyDuration(duration), bitRate));

            return response;
        });
    }

    @Override
    public void deleteObject(String bucket, String key) {
        AwsErrorHandling.decorate(
                () -> "Failed to delete object %s/%s".formatted(bucket, key),
                () -> s3.deleteObject(builder -> builder.bucket(bucket).key(key))
        );
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectRequest request) {
        String srcBucket = request.sourceBucket();
        String srcKey = request.sourceKey();
        String destBucket = request.destinationBucket();
        String destKey = request.destinationKey();

        return AwsErrorHandling.decorate(
                () -> "Failed copying object s3://%s/%s to s3://%s/%s".formatted(srcBucket, srcKey, destBucket, destKey),
                () -> {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    CopyObjectResponse response = s3.copyObject(request);
                    log.info("Done copying object from s3://{}/{} to s3://{}/{} in {}",
                            srcBucket, srcKey, destBucket, destKey, logFriendlyDuration(stopwatch.elapsed()));
                    return response;
                }
        );
    }

    private static <T> T getFromS3(String bucket, String key, BiFunction<String, String, T> s3Call, String outputDescription) {
        return AwsErrorHandling.decorate(() -> "Failed to get %s from %s/%s".formatted(outputDescription, bucket, key), () -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            T result = s3Call.apply(bucket, key);
            log.info("Retrieved {} from {}/{} in {}", outputDescription, bucket, key, logFriendlyDuration(stopwatch.elapsed()));
            return result;
        });
    }
}
