package storage.cloud.cloudstorage.service.resource;

import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@RequiredArgsConstructor
@Service
public class ResourceInfoService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public ResourceResponse resourceInfo(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPath = buildPreparedPath(preparedRoot, path);

        validateResourceExists(
                minioRepository.doesPathExist(fullPath), fullPath
        );

        String type = path.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();
        if ("FILE".equals(type)) {
            StatObjectResponse objectResponse = minioRepository.getObjectResponse(fullPath);

            String name = extractName(path);
            String parentPath = extractParentPathForFile(path);
            return ResourceResponse.builder()
                    .path(parentPath)
                    .name(name)
                    .size(objectResponse.size())
                    .type(Type.FILE.name())
                    .build();
        } else {
            FolderPathParts result = getResult(path, fullPath);

            return ResourceResponse.builder()
                    .path(result.resourceParentPath())
                    .name(result.folderName())
                    .type(Type.DIRECTORY.name())
                    .build();
        }
    }
}
