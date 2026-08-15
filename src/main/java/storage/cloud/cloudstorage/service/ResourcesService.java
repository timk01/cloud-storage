package storage.cloud.cloudstorage.service;

import io.minio.ObjectWriteResponse;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
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
        String fullPath = buildPreparedPath(path, userId);
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
        String fullPath = buildPreparedPath(path, userId);
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
    private String buildPreparedPath(String path, Long userId) { //подумать над именеем.... это не совсем оно
        String[] splitBucket = minioBucketName.split("-");
        String userRoot = splitBucket[0] + "-" + userId + "-" + splitBucket[1];

        return userRoot + "/" + path;
    }

    public List<ResourceResponse> upload(String path, MultipartFile[] files, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String preparedPath = buildPreparedPath(path, userId);
        initializer.initStorage(preparedPath);

        List<MultipartFile> cleanFiles = new ArrayList<>();
        List<String> cleanFullPaths = new ArrayList<>();
        for (MultipartFile file : files) {

            String uncheckedFilename = file.getOriginalFilename();
            if (uncheckedFilename == null || uncheckedFilename.isEmpty()) {
                continue;
            }

            String cleanedPathName = StringUtils.cleanPath(uncheckedFilename);

            String fullPath = preparedPath + cleanedPathName;

            minioRepository.checkFile(fullPath); //даже 1 прокунтый вариант - гг

            cleanFullPaths.add(fullPath);
        }

        List<ObjectWriteResponse> uploaded = minioRepository.upload(List.of(files), cleanFullPaths);

        for (ObjectWriteResponse response : uploaded) {

        }

        //ИЛИ ПРОЩЕ::

/*        resources.add(
                ResourceResponse.builder()
                        .path(path)                    // Ваша переменная пути
                        .name(file.getOriginalFilename()) // Или имя из currentPath
                        .size(file.getSize())          // РАЗМЕР БЕРЕМ НАПРЯМУЮ ИЗ MULTIPARTFILE!
                        .type(Type.FILE.name())
                        .build()
        );*/
/*
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
            }*/
        }


        return null;
    }

}
