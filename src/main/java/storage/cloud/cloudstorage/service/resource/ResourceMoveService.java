package storage.cloud.cloudstorage.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.exception.managed.DestinationResourceAlreadyExistsException;
import storage.cloud.cloudstorage.exception.managed.ResourceMoveConflictException;
import storage.cloud.cloudstorage.exception.managed.ResourceTypeMismatchException;
import storage.cloud.cloudstorage.exception.managed.SourceAndDestinationAreEqualException;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceMoveService {
    private final ObjectStorage storage;
    private final MinioProperties properties;
    private final StorageInitializer initializer;

    public ResourceResponse move(String fromPath, String toPath, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, properties.bucket().name());
        initializer.initStorage(preparedRoot);

        String fullPathFrom = buildPreparedPath(preparedRoot, fromPath);
        String fullPathTo = buildPreparedPath(preparedRoot, toPath);
        String fromType = fromPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();
        String toType = toPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

        validateResourceExists(
                storage.doesPathExist(fullPathFrom), fullPathFrom
        );

        validateSourceAndDestinationAreDistinct(fullPathFrom, fullPathTo);

        validateResourcesTypes(fromType, toType);

        validateDestination(fullPathTo);

        validateDirectoryPaths(fromPath, toPath, fromType);

        ResourceResponse response = moveResource(toPath, fromType, fullPathFrom, fullPathTo);

        log.info(
                "Resource is moved for user: userId={}, fromPath={}, toPath={}; with type={}",
                userId,
                fromPath,
                toPath,
                fromType
        );

        return response;
    }

    private void validateSourceAndDestinationAreDistinct(String fullPathFrom, String fullPathTo) {
        if (fullPathFrom.equals(fullPathTo)) {
            throw new SourceAndDestinationAreEqualException(
                    String.format(
                            "Source and destination are equal by path: %s", fullPathTo
                    )
            );
        }
    }

    private void validateResourcesTypes(String fromType, String toType) {
        if (!fromType.equals(toType)) {
            throw new ResourceTypeMismatchException("Source and destination types are different");
        }
    }

    private void validateDestination(String fullPathTo) {
        if (storage.doesPathExist(fullPathTo)) {
            throw new DestinationResourceAlreadyExistsException(
                    String.format(
                            "Resource already exists by path: %s", fullPathTo
                    )
            );
        }
    }

    private void validateDirectoryPaths(String fromPath, String toPath, String fromType) {
        if (fromType.equals("DIRECTORY") && toPath.startsWith(fromPath)) {
            throw new ResourceMoveConflictException(
                    String.format(
                            "Resource cannot moved by path " +
                                    "since it's impossible to move folder into it's subdirectory: %s", toPath
                    )
            );
        }
    }

    private ResourceResponse moveResource(String toPath, String fromType, String fullPathFrom, String fullPathTo) {
        if ("FILE".equals(fromType)) {
            long resourceSize = storage.retrieveResourceSize(fullPathFrom);

            storage.moveFile(fullPathFrom, fullPathTo);

            String name = extractName(toPath);
            String path = extractParentPathForFile(toPath);
            return ResourceResponse.builder()
                    .path(path)
                    .name(name)
                    .size(resourceSize)
                    .type(Type.FILE.name())
                    .build();
        } else {
            storage.moveDirectory(fullPathFrom, fullPathTo);

            FolderPathParts result = getResult(toPath, fullPathTo);
            return ResourceResponse.builder()
                    .path(result.resourceParentPath())
                    .name(result.folderName())
                    .type(Type.DIRECTORY.name())
                    .build();
        }
    }
}
