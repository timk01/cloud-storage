package storage.cloud.cloudstorage.service;

import io.minio.errors.MinioException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.response.CreatedFolderResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@Service
public class ResourcesService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;

    public CreatedFolderResponse createFolder(String path, Long userId) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        String[] split = minioBucketName.split("-");

        String createBucketName;
        createBucketName = split[0] + "-" + userId + "-" + split[1];

        String fullPath = createBucketName + "/" + path;

        //String filePath = "C:\\Users\\timk0\\Downloads\\gorgon.jpg";
        minioRepository.creaTeFile(fullPath);

        return null;
    }
}
