package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import storage.cloud.cloudstorage.exception.technical.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RequiredArgsConstructor
@Component
public class StorageInitializer {

    private final MinioClient minioClient;
    private final String minioBucketName;

    public void initStorage(String fullPath) {
        try {
            initBucket();
            initRoot(fullPath);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    private void initBucket() throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
            XmlParserException {
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

            log.info(
                    "MinIO bucket is created: bucketName={}",
                    minioBucketName
            );
        }
    }

    private void initRoot(String root) throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
            XmlParserException {
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

            log.info(
                    "MinIO bucket is created: bucketName={}",
                    minioBucketName
            );
        }
    }
}
