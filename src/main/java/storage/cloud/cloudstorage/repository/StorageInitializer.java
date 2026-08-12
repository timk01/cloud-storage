package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@Component
public class StorageInitializer {

    private final MinioClient minioClient;
    private final String minioBucketName;

    public void initStorage(String fullPath) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        initBucket();
        initRoot(fullPath);
    }

    private void initBucket() throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        boolean doesBucketExist = minioClient.bucketExists(
                BucketExistsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .build()
        );
        if (!doesBucketExist) {
            minioClient.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(minioBucketName)
                            .build()
            );
        }
    }

    private void initRoot(String fullPath) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        String root = fullPath.split("/")[0] + "/";

        Iterable<Result<Item>> rootResults = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(root)
                        .maxKeys(1)
                        .build()
        );

        boolean hasRoot = rootResults.iterator().hasNext();

        if (!hasRoot) {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(root)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build()
            );
        }
    }
}
