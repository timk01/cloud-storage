package storage.cloud.cloudstorage.service.resource;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@RequiredArgsConstructor
@Service
public class ResourceDeleteService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public void delete(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPathTo = buildPreparedPath(preparedRoot, path);

        validateResourceExists(minioRepository.doesPathExist(fullPathTo), fullPathTo);

        String type = path.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

        deleteResources(type, fullPathTo);
    }

    private void deleteResources(String type, String fullPathTo) {
        if ("FILE".equals(type)) {
            minioRepository.deleteFile(fullPathTo);
        } else {
            List<Item> searchResult = minioRepository.search(fullPathTo);

            List<String> filesPath = new ArrayList<>();
            List<String> directoriesPath = new ArrayList<>();
            fillDirectoryPaths(searchResult, directoriesPath, filesPath);

            minioRepository.deleteResources(filesPath, directoriesPath);
        }
    }

    private void fillDirectoryPaths(List<Item> searchResult, List<String> directoriesPath, List<String> filesPath) {
        for (Item item : searchResult) {
            String pathToResource = item.objectName();
            if (pathToResource.endsWith("/")) {
                directoriesPath.add(pathToResource);
            } else {
                filesPath.add(pathToResource);
            }
        }
        directoriesPath.sort(Collections.reverseOrder());
    }
}
