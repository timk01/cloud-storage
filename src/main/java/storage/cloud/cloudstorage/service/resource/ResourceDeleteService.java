package storage.cloud.cloudstorage.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceDeleteService {

    private final ObjectStorage storage;
    private final MinioProperties properties;

    public void delete(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);

        String fullPathTo = buildPreparedPath(preparedRoot, path);

        validateResourceExists(storage.doesPathExist(fullPathTo), fullPathTo);

        Type type = path.endsWith("/") ? Type.DIRECTORY : Type.FILE;

        deleteResources(type, fullPathTo);

        log.info(
                "Resource is deleted for user: userId={}, path={}; with type={}",
                userId,
                path,
                type.name()
        );
    }

    private void deleteResources(Type type, String fullPathTo) {
        if (Type.FILE == type) {
            storage.deleteFile(fullPathTo);
        } else {
            List<StorageItem> storageItems = storage.retrieveItemsRecursively(fullPathTo);

            List<String> filesPath = new ArrayList<>();
            List<String> directoriesPath = new ArrayList<>();
            fillDirectoryPaths(storageItems, directoriesPath, filesPath);

            storage.deleteResources(filesPath, directoriesPath);
        }
    }

    private void fillDirectoryPaths(List<StorageItem> searchResult, List<String> directoriesPath, List<String> filesPath) {
        for (StorageItem item : searchResult) {
            String pathToResource = item.resourcePath();
            if (item.directory()) {
                directoriesPath.add(pathToResource);
            } else {
                filesPath.add(pathToResource);
            }
        }
        directoriesPath.sort(Collections.reverseOrder());
    }
}
