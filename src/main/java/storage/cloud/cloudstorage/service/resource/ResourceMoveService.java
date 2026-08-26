package storage.cloud.cloudstorage.service.resource;

import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.exception.DestinationResourceAlreadyExistsException;
import storage.cloud.cloudstorage.exception.ResourceMoveConflictException;
import storage.cloud.cloudstorage.exception.ResourceTypeMismatchException;
import storage.cloud.cloudstorage.exception.SourceAndDestinationAreEqualException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@RequiredArgsConstructor
@Service
public class ResourceMoveService {
    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public ResourceResponse move(String fromPath, String toPath, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPathFrom = buildPreparedPath(preparedRoot, fromPath);
        String fullPathTo = buildPreparedPath(preparedRoot, toPath);
        String fromType = fromPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();
        String toType = toPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

        validateResourceExists(
                minioRepository.doesPathExist(fullPathFrom), fullPathFrom
        );

        validateSourceAndDestinationAreDistinct(fullPathFrom, fullPathTo);

        validateResourcesTypes(fromType, toType);

        validateDestination(fullPathTo);

        validateDirectoryPaths(fromPath, toPath, fromType);

        return moveResource(toPath, fromType, fullPathFrom, fullPathTo);
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
        if (minioRepository.doesPathExist(fullPathTo)) {
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
            StatObjectResponse objectResponse = minioRepository.getObjectResponse(fullPathFrom);

            minioRepository.moveFile(fullPathFrom, fullPathTo);

            String name = extractName(toPath);
            String path = extractParentPathForFile(toPath);
            return ResourceResponse.builder()
                    .path(path)
                    .name(name)
                    .size(objectResponse.size())
                    .type(Type.FILE.name())
                    .build();
        } else {
            minioRepository.moveDirectory(fullPathFrom, fullPathTo);

            FolderPathParts result = getResult(toPath, fullPathTo);
            return ResourceResponse.builder()
                    .path(result.resourceParentPath())
                    .name(result.folderName())
                    .type(Type.DIRECTORY.name())
                    .build();
        }
    }
}
