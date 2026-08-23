package storage.cloud.cloudstorage.service.resource;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@RequiredArgsConstructor
@Service
public class ResourceSearchService {
    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public List<ResourceResponse> search(String query, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        List<Item> searchResult = minioRepository.search(preparedRoot);
        List<ResourceResponse> resources = new ArrayList<>();
        for (Item item : searchResult) {
            String fullPathTillItem = item.objectName();
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
                String fileName = parseFileName(item);
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

        return resources;
    }

    private String parseFileName(Item item) {
        String fullname = item.objectName();
        return extractName(fullname);
    }
}
