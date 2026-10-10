package storage.cloud.cloudstorage.service.directory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DirectoryCreateService {
    private final ObjectStorage storage;
    private final MinioProperties properties;

    public ResourceResponse createFolder(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);

        String fullPath = buildPreparedPath(preparedRoot, path);
        FolderPathParts result = getResult(path, fullPath);

        storage.creaTeFolder(result.minioParentPath(), fullPath);

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
