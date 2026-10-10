package storage.cloud.cloudstorage.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.repository.StorageItem;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceSearchService {
    private final ObjectStorage storage;
    private final MinioProperties properties;
    private final StorageInitializer initializer;

    public List<ResourceResponse> search(String query, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, properties.bucket().name());
        initializer.initStorage(preparedRoot);

        List<StorageItem> storageItems = storage.retrieveItemsRecursively(preparedRoot);
        List<ResourceResponse> resources = new ArrayList<>();
        for (StorageItem item : storageItems) {
            String fullPathTillItem = item.resourcePath();
            String itemFullPathWithoutRoot = fullPathTillItem.replace(preparedRoot, "");

            String normalizedQuery = query.toLowerCase();
            if (itemFullPathWithoutRoot.endsWith("/")) {

                FolderPathParts result = getResult(itemFullPathWithoutRoot, fullPathTillItem);

                String folderName = result.folderName();
                if (folderName.toLowerCase().contains(normalizedQuery)) {
                    resources.add(
                            ResourceResponse
                                    .builder()
                                    .path(result.resourceParentPath())
                                    .name(folderName)
                                    .type(Type.DIRECTORY.name())
                                    .build()
                    );
                }
            } else {
                String fileName = parseFileName(item.resourcePath());
                if (fileName.toLowerCase().contains(normalizedQuery)) {
                    String path = itemFullPathWithoutRoot.replace(fileName, "");
                    resources.add(
                            ResourceResponse
                                    .builder()
                                    .path(path)
                                    .name(fileName)
                                    .size(item.size())
                                    .type(Type.FILE.name())
                                    .build()
                    );
                }
            }
        }

        log.info(
                "Resource search is completed for user: userId={}, query={}; with resourcesCount={}",
                userId,
                query,
                resources.size()
        );

        return resources;
    }

    private String parseFileName(String resourcePath) {
        return extractName(resourcePath);
    }
}
