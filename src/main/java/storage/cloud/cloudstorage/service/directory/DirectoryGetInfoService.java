package storage.cloud.cloudstorage.service.directory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DirectoryGetInfoService {
    private final ObjectStorage storage;
    private final MinioProperties properties;

    public List<ResourceResponse> getFolderInfo(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);

        String fullPath = buildPreparedPath(preparedRoot, path);
        List<StorageItem> storageItems = storage.retrieveDirectoryItems(fullPath);

        List<ResourceResponse> resources = new ArrayList<>();
        for (StorageItem item : storageItems) {
            if (fullPath.equals(item.resourcePath())) {
                continue;
            }
            if (item.directory()) {
                resources.add(
                        ResourceResponse
                                .builder()
                                .path(path)
                                .name(parseDirName(item.resourcePath()))
                                .type(Type.DIRECTORY.name())
                                .build()
                );
            } else {
                resources.add(
                        ResourceResponse
                                .builder()
                                .path(path)
                                .name(parseFileName(item.resourcePath()))
                                .size(item.size())
                                .type(Type.FILE.name())
                                .build()
                );
            }
        }

        log.info(
                "Folder info is received for user: userId={}, path={};" +
                        " with resourcesCount={}",
                userId,
                path,
                resources.size()
        );

        return resources;
    }

    private String parseDirName(String resourcePath) {
        String trimmedFullName = removeTrailingSlash(resourcePath);
        return extractName(trimmedFullName);
    }

    private String parseFileName(String resourcePath) {
        return extractName(resourcePath);
    }
}
