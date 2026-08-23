package storage.cloud.cloudstorage.service.directory;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
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
public class DirectoryGetInfoService {
    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public List<ResourceResponse> getFolderInfo(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPath = buildPreparedPath(preparedRoot, path);
        List<Item> items = minioRepository.getFolderInfo(fullPath);

        List<ResourceResponse> resources = new ArrayList<>();
        for (Item item : items) {
            if (fullPath.equals(item.objectName())) {
                continue;
            }
            if (item.isDir()) {
                resources.add(
                        ResourceResponse
                                .builder()
                                .path(path)
                                .name(parseDirName(item))
                                .type(Type.DIRECTORY.name())
                                .build()
                );
            } else {
                resources.add(
                        ResourceResponse
                                .builder()
                                .path(path)
                                .name(parseFileName(item))
                                .size(item.size())
                                .type(Type.FILE.name())
                                .build()
                );
            }
        }

        return resources;
    }

    private String parseDirName(Item item) {
        String fullname = item.objectName();
        String trimmedFullName = removeTrailingSlash(fullname);
        return extractName(trimmedFullName);
    }

    private String parseFileName(Item item) {
        String fullname = item.objectName();
        return extractName(fullname);
    }
}
