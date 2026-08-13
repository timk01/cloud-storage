package storage.cloud.cloudstorage.service;

import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ResourcesService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public ResourceResponse createFolder(String path, Long userId) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        String fullPath = buildFullPath(path, userId);
        initializer.initStorage(fullPath);

        FolderPathParts result = getResult(path, fullPath);

        minioRepository.creaTeFolder(result.parentFolder(), fullPath);

        return ResourceResponse
                .builder()
                .path(result.folderPath())
                .name(result.folderName())
                .type(Type.DIRECTORY.name())
                .build();
    }

    @NotNull
    private FolderPathParts getResult(String path, String fullPath) {
        String trimmedFullPath = removeTrailingSlash(fullPath);
        int lastFolderIndex = trimmedFullPath.lastIndexOf("/");
        String parentFolder = trimmedFullPath.substring(0, lastFolderIndex + 1);

        String folderPath = extractParentPath(path);
        String lastFolder = fullPath.substring(lastFolderIndex + 1);
        String folderName = removeTrailingSlash(lastFolder);

        return new FolderPathParts(parentFolder, folderPath, folderName);
    }

    private record FolderPathParts(
            String parentFolder,
            String folderPath,
            String folderName
    ) {
    }

    @NotNull
    private String extractParentPath(String path) {
        String trimmedOriginalPath = removeTrailingSlash(path);
        int lastFolderIndex = trimmedOriginalPath.lastIndexOf("/");
        return trimmedOriginalPath.substring(0, lastFolderIndex + 1);
    }

    public List<ResourceResponse> getFolderInfo(String path, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fullPath = buildFullPath(path, userId);
        initializer.initStorage(fullPath);

        Iterable<Result<Item>> folderInfo = minioRepository.getFolderInfo(fullPath);

        List<ResourceResponse> resources = new ArrayList<>();
        for (Result<Item> itemResult : folderInfo) {
            Item item = itemResult.get();
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

    @NotNull
    private String parseFileName(Item item) {
        String fullname = item.objectName();
        return extractName(fullname);
    }

    @NotNull
    private String parseDirName(Item item) {
        String fullname = item.objectName();
        String trimmedFullName = removeTrailingSlash(fullname);
        return extractName(trimmedFullName);
    }

    @NotNull
    private String removeTrailingSlash(String fullname) {
        return fullname.substring(0, fullname.length() - 1);
    }

    @NotNull
    private String extractName(String fullname) {
        return fullname.substring(fullname.lastIndexOf("/") + 1);
    }

    @NotNull
    private String buildFullPath(String path, Long userId) {
        String[] splitBucket = minioBucketName.split("-");
        String userRoot = splitBucket[0] + "-" + userId + "-" + splitBucket[1];

        return userRoot + "/" + path;
    }
}
