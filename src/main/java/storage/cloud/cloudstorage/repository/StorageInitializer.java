package storage.cloud.cloudstorage.repository;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import storage.cloud.cloudstorage.config.MinioProperties;
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
    private final MinioProperties properties;
    private final MinioRepository minioRepository;

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
                        .bucket(properties.bucket().name())
                        .build()
        );
        if (!doesBucketExist) {
            minioClient.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(properties.bucket().name())
                            .build()
            );

            log.info(
                    "MinIO bucket is created: bucketName={}",
                    properties.bucket().name()
            );
        }
    }

    private void initRoot(String root) throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
            XmlParserException {

        if (!minioRepository.doesPrefixExist(root)) {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(properties.bucket().name())
                            .object(root)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build()
            );

            log.info(
                    "MinIO bucket is created: bucketName={}",
                    properties.bucket().name()
            );
        }
    }
}
