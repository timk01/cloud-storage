package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@Repository
public class MinioRepository {

    private final MinioClient minioClient;
    private final String minioBucketName;


    public void creaTeFile(String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        //String bucketName = "testbucket1";
        /*        try {*/

        //при первом запросе - создаем бакет (лениво)
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
        } else {
            System.out.println("We have bucket already!");
        }

        //при первом запросе (лениво) создаем рут.

        String root = fullPath.split("/")[0] + "/";

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(root)
                        .maxKeys(1)
                        .build()
        );

        boolean hasRoot = results.iterator().hasNext();

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



/*            minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object("gorgon-uploaded.jpg")
                            .filename(fileName)
                            .build()
            );
            System.out.println("Gorgon is uploaded!");*/

/*        } catch (MinioException em) {
            //throw new MinioException("Cannot login with such credentials");
            System.out.println("Error occurred: " + em);
            System.out.println("HTTP trace: " + em.httpTrace());
        }*/
    }
}
