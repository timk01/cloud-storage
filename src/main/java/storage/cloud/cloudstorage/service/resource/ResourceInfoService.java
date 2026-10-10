package storage.cloud.cloudstorage.service.resource;

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
public class ResourceInfoService {

    private final ObjectStorage storage;
    private final MinioProperties properties;

    public ResourceResponse resourceInfo(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, properties.bucket().name());

        String fullPath = buildPreparedPath(preparedRoot, path);

        validateResourceExists(
                storage.doesPathExist(fullPath), fullPath
        );

        String type = path.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();
        if ("FILE".equals(type)) {
            long resourceSize = storage.retrieveResourceSize(fullPath);
            String name = extractName(path);
            String parentPath = extractParentPathForFile(path);

            log.info(
                    "Resource info is received for user: userId={}, path={}; with type={}",
                    userId,
                    path,
                    type
            );

            return ResourceResponse.builder()
                    .path(parentPath)
                    .name(name)
                    .size(resourceSize)
                    .type(Type.FILE.name())
                    .build();
        } else {
            FolderPathParts result = getResult(path, fullPath);

            log.info(
                    "Resource info is received for user: userId={}, path={}; with type={}",
                    userId,
                    path,
                    type
            );

            return ResourceResponse.builder()
                    .path(result.resourceParentPath())
                    .name(result.folderName())
                    .type(Type.DIRECTORY.name())
                    .build();
        }
    }
}
