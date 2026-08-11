package storage.cloud.cloudstorage.config;

import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.user}")
    private String minioUser;

    @Value("${minio.password}")
    private String minioPassword;

    @Value("${minio.bucket.name}")
    private String minBucketName;

    @Bean
    public MinioClient minioClient() {
        //try {
            return MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials(minioUser, minioPassword)
                    .build();
/*        } catch (MinioException me) {
            throw new MinioLoginException("Cannot login with such credentials");
        }*/
    }

    @Bean
    public String minioBucketName() {
        return this.minBucketName;
    }
}
