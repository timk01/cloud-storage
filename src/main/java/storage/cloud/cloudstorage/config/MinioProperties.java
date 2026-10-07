package storage.cloud.cloudstorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "minio")
public record MinioProperties(
        String url,
        String user,
        String password,
        Bucket bucket
) {
    public record Bucket(String name) {
    }
}
