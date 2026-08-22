package storage.cloud.cloudstorage.service;

import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.*;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ResourcesService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public ResourceResponse createFolder(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        String fullPath = buildPreparedPath(preparedRoot, path);
        FolderPathParts result = getResult(path, fullPath);

        minioRepository.creaTeFolder(result.minioParentPath(), fullPath);

        return ResourceResponse
                .builder()
                .path(result.resourceParentPath())
                .name(result.folderName())
                .type(Type.DIRECTORY.name())
                .build();
    }

    @NotNull
    private FolderPathParts getResult(String resourcePath, String fullMinioPath) {
        String trimmedFullPath = removeTrailingSlash(fullMinioPath);
        int lastFolderIndex = trimmedFullPath.lastIndexOf("/");
        String minioParentPath = trimmedFullPath.substring(0, lastFolderIndex + 1);

        String resourceParentPath = extractParentPathForFolder(resourcePath);
        String lastFolder = fullMinioPath.substring(lastFolderIndex + 1);
        String folderName = removeTrailingSlash(lastFolder);

        return new FolderPathParts(minioParentPath, resourceParentPath, folderName);
    }

    private record FolderPathParts(
            String minioParentPath,
            String resourceParentPath,
            String folderName
    ) {


    }

    @NotNull
    private String extractParentPathForFolder(String folderPath) {
        String trimmedOriginalPath = removeTrailingSlash(folderPath);
        return extractParentPathForFile(trimmedOriginalPath);
    }

    @NotNull
    private String extractParentPathForFile(String filePath) {
        int lastFolderIndex = filePath.lastIndexOf("/");
        return filePath.substring(0, lastFolderIndex + 1);
    }

    public List<ResourceResponse> getFolderInfo(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);
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
    private String buildPreparedRoot(Long userId) {
        String[] splitBucket = minioBucketName.split("-");
        return splitBucket[0] + "-" + userId + "-" + splitBucket[1] + "/";
    }

    @NotNull
    private String buildPreparedPath(String preparedRoot, String path) {
        return preparedRoot + path;
    }

    public List<ResourceResponse> upload(String path, MultipartFile[] files, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        String preparedPath = buildPreparedPath(preparedRoot, path);
        List<PreparedFile> preparedFiles = getPreparedFiles(files, preparedPath);

        List<String> fullPathTillFile = preparedFiles.stream()
                .map((preparedFile) -> preparedFile.fullPathTillFile)
                .toList();

        minioRepository.checkFiles(fullPathTillFile);

        List<MultipartFile> multipartFiles = preparedFiles.stream()
                .map((preparedFile) -> preparedFile.multipartFile)
                .toList();
        minioRepository.upload(multipartFiles, fullPathTillFile);

        List<ResourceResponse> resources = new ArrayList<>();
        for (PreparedFile file : preparedFiles) {
            resources.add(
                    ResourceResponse
                            .builder()
                            .path(path)
                            .name(file.cleanedPathName())
                            .size(file.multipartFile.getSize())
                            .type(Type.FILE.name())
                            .build()
            );
        }

        return resources;
    }

    @NotNull
    private List<PreparedFile> getPreparedFiles(MultipartFile[] files, String preparedPath) {
        List<PreparedFile> preparedFiles = new ArrayList<>();
        for (MultipartFile multipartFile : files) {

            String uncheckedFilename = multipartFile.getOriginalFilename();
            if (uncheckedFilename == null || uncheckedFilename.isEmpty()) {
                continue;
            }

            String cleanedPathName = StringUtils.cleanPath(uncheckedFilename);

            validateCleanedPathName(cleanedPathName);

            String fullPathTillFile = preparedPath + cleanedPathName;

            preparedFiles.add(
                    new PreparedFile(
                            cleanedPathName,
                            fullPathTillFile,
                            multipartFile
                    )
            );
        }
        return preparedFiles;
    }

    private void validateCleanedPathName(String cleanedPathName) {
        if (cleanedPathName.isEmpty() || ".".equals(cleanedPathName) || "..".equals(cleanedPathName)) {
            throw new InvalidFileNameException(
                    String.format(
                            "Multipartfile is a invalid: %s ", cleanedPathName
                    )
            );
        }
    }

    public record PreparedFile(
            String cleanedPathName,
            String fullPathTillFile,
            MultipartFile multipartFile
    ) {


    }

    public List<ResourceResponse> search(String query, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        List<Item> searchResult = minioRepository.search(preparedRoot);
        List<ResourceResponse> resources = new ArrayList<>();
        for (Item item : searchResult) {
            String fullPathTillItem = item.objectName();
            String itemFullPathWithoutRoot = fullPathTillItem.replace(preparedRoot, "");

            String normalizedQuery = query.toLowerCase();
            if (itemFullPathWithoutRoot.endsWith("/")) {

                FolderPathParts result = getResult(itemFullPathWithoutRoot, fullPathTillItem);

                String folderName = result.folderName;
                if (folderName.toLowerCase().contains(normalizedQuery)) {
                    resources.add(
                            ResourceResponse
                                    .builder()
                                    .path(result.resourceParentPath)
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

    public ResourceResponse move(String fromPath, String toPath, Long userId) {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        String fullPathFrom = buildPreparedPath(preparedRoot, fromPath);
        String fullPathTo = buildPreparedPath(preparedRoot, toPath);
        String fromType = fromPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();
        String toType = toPath.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

        validateSource(fullPathFrom);

        validateSourceAndDestinationAreDistinct(fullPathFrom, fullPathTo);

        validateResourcesTypes(fromType, toType);

        validateDestination(fullPathTo);

        validateDirectoryPaths(fromPath, toPath, fromType);

        return moveResource(toPath, fromType, fullPathFrom, fullPathTo);
    }

    private void validateSource(String fullPathFrom) {
        if (!minioRepository.doesPathExist(fullPathFrom)) {
            throw new SourceResourceNotFoundException(
                    String.format(
                            "Resource is not found by path: %s", fullPathFrom
                    )
            );
        }
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
                    .path(result.resourceParentPath)
                    .name(result.folderName)
                    .type(Type.DIRECTORY.name())
                    .build();
        }
    }
}
