package storage.cloud.cloudstorage.service;

import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.InvalidFileNameException;
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

        String resourceParentPath = extractParentPath(resourcePath);
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
    private String extractParentPath(String path) {
        String trimmedOriginalPath = removeTrailingSlash(path);
        int lastFolderIndex = trimmedOriginalPath.lastIndexOf("/");
        return trimmedOriginalPath.substring(0, lastFolderIndex + 1);
    }

    public List<ResourceResponse> getFolderInfo(String path, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        String fullPath = buildPreparedPath(preparedRoot, path);
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
    private String buildPreparedRoot(Long userId) {
        String[] splitBucket = minioBucketName.split("-");
        return splitBucket[0] + "-" + userId + "-" + splitBucket[1] + "/";
    }

    @NotNull
    private String buildPreparedPath(String preparedRoot, String path) {
        return preparedRoot + path;
    }

    public List<ResourceResponse> upload(String path, MultipartFile[] files, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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
    public List<ResourceResponse> search(String query, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String preparedRoot = buildPreparedRoot(userId);
        initializer.initStorage(preparedRoot);

        Iterable<Result<Item>> searchResult = minioRepository.search(preparedRoot);
        List<ResourceResponse> resources = new ArrayList<>();
        for (Result<Item> itemResult : searchResult) {
            Item item = itemResult.get();
            String fullPathTillItem = item.objectName(); //user-1-files/folder1/my-gorgon-folder/
            String itemFullPathWithoutRoot = fullPathTillItem.replace(preparedRoot, ""); //folder1/my-gorgon-folder/

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
}
