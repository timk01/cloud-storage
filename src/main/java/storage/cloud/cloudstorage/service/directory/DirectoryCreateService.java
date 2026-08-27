package storage.cloud.cloudstorage.service.directory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DirectoryCreateService {
    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public ResourceResponse createFolder(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPath = buildPreparedPath(preparedRoot, path);
        FolderPathParts result = getResult(path, fullPath);

        minioRepository.creaTeFolder(result.minioParentPath(), fullPath);

        log.info(
                "Folder is created for user: userId={}, original path={};" +
                        " with ResourceResponse: parentPath={}, folderName={}, type={}",
                userId,
                path,
                result.resourceParentPath(),
                result.folderName(),
                Type.DIRECTORY.name()
        );

        return ResourceResponse
                .builder()
                .path(result.resourceParentPath())
                .name(result.folderName())
                .type(Type.DIRECTORY.name())
                .build();
    }
}
