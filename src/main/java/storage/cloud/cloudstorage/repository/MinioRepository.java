package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.managed.FileAlreadyExistsException;
import storage.cloud.cloudstorage.exception.managed.FolderAlreadyExistsException;
import storage.cloud.cloudstorage.exception.managed.FolderNotFoundException;
import storage.cloud.cloudstorage.exception.managed.ParentFolderHasNotFoundException;
import storage.cloud.cloudstorage.exception.technical.ResourceDeletionException;
import storage.cloud.cloudstorage.exception.technical.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class MinioRepository {

    private final MinioClient minioClient;
    private final String minioBucketName;

    public void creaTeFolder(String minioParentPath, String fullPath) {
        checkParentFolder(minioParentPath);

        try {
            if (doesPathExist(fullPath)) {
                throw new FolderAlreadyExistsException(
                        String.format(
                                "Folder already exists: %s", fullPath
                        )
                );
            }

            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(fullPath)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build()
            );
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    private void checkParentFolder(String parentFolder) {
        Iterable<Result<Item>> parentResults = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(parentFolder)
                        .maxKeys(1)
                        .build()
        );

        boolean hasParent = parentResults.iterator().hasNext();

        if (!hasParent) {
            throw new ParentFolderHasNotFoundException(
                    String.format(
                            "No parent folder has been found: %s", parentFolder
                    )
            );
        }
    }

    public List<Item> getFolderInfo(String fullPath) {
        try {
            Iterable<Result<Item>> folderResults = minioClient.listObjects(
                    ListObjectsArgs
                            .builder()
                            .bucket(minioBucketName)
                            .prefix(fullPath)
                            .recursive(false)
                            .build()
            );

            checkFolderExists(fullPath, folderResults);

            List<Item> items = new ArrayList<>();
            for (Result<Item> itemResult : folderResults) {
                items.add(itemResult.get());
            }

            return items;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    private void checkFolderExists(String fullPath, Iterable<Result<Item>> folderResults) {
        boolean hasFolder = folderResults.iterator().hasNext();

        if (!hasFolder) {
            throw new FolderNotFoundException(
                    String.format(
                            "Folder is not found: %s ", fullPath
                    ));
        }
    }

    public void checkFiles(List<String> fullPaths) {
        try {
            for (String fullPath : fullPaths) {
                try {
                    minioClient.statObject(
                            StatObjectArgs
                                    .builder()
                                    .bucket(minioBucketName)
                                    .object(fullPath)
                                    .build()
                    );

                    throw new FileAlreadyExistsException(
                            String.format(
                                    "File already exists: %s ", fullPath
                            )
                    );
                } catch (ErrorResponseException ere) {
                    if ("NoSuchKey".equals(ere.errorResponse().code())) {
                        continue;
                    }
                    throw ere;
                }
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public void upload(List<MultipartFile> files, List<String> fullPathTillFiles) {
        try {
            createFoldersRecursively(fullPathTillFiles);

            int index = 0;
            for (MultipartFile file : files) {
                minioClient.putObject(
                        PutObjectArgs
                                .builder()
                                .bucket(minioBucketName)
                                .object(fullPathTillFiles.get(index++))
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public List<Item> search(String preparedRoot) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs
                            .builder()
                            .bucket(minioBucketName)
                            .prefix(preparedRoot)
                            .recursive(true)
                            .build()
            );

            List<Item> items = new ArrayList<>();
            for (Result<Item> itemResult : results) {
                items.add(itemResult.get());
            }

            return items;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public void moveFile(String fromPath, String toPath) {
        try {
            createFoldersRecursively(List.of(toPath));

            List<String> copiedResources = new ArrayList<>();
            copyResource(List.of(fromPath), List.of(toPath), copiedResources);

            try {
                removeResources(List.of(fromPath));
            } catch (IOException deletionException) {
                try {
                    removeResource(toPath);
                } catch (IOException rollbackException) {
                    throw new StorageException(
                            "Cannot rollback file move",
                            rollbackException
                    );
                }

                throw deletionException;
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public StatObjectResponse getObjectResponse(String pathTillObject) {
        try {
            return minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(pathTillObject)
                            .build()
            );
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public void moveDirectory(String fromPath, String toPath) {
        try {
            List<String> fullPathTillResourceOldLocations;
            List<String> fullPathTillResourceNewLocations;
            List<String> createdFoldersRecursively;
            List<Item> searchResultsFromOldPath = search(fromPath);

            fullPathTillResourceOldLocations = new ArrayList<>();
            fullPathTillResourceNewLocations = new ArrayList<>();
            prepareLocations(
                    fromPath,
                    toPath,
                    searchResultsFromOldPath,
                    fullPathTillResourceOldLocations,
                    fullPathTillResourceNewLocations
            );

            createdFoldersRecursively = createFoldersRecursively(fullPathTillResourceNewLocations);

            List<String> copiedResources = new ArrayList<>();
            try {
                copyResource(fullPathTillResourceOldLocations, fullPathTillResourceNewLocations, copiedResources);
            } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
                rollBackCopiedResources(copiedResources, createdFoldersRecursively);

                throw e;
            }

            removeResources(fullPathTillResourceOldLocations);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    private void prepareLocations(
            String fromPath,
            String toPath,
            List<Item> searchResultsFromOldPath,
            List<String> fullPathTillResourceOldLocations,
            List<String> fullPathTillResourceNewLocations
    ) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException,
            InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        for (Item itemResult : searchResultsFromOldPath) {
            String oldLocationObjectName = itemResult.objectName();
            fullPathTillResourceOldLocations.add(oldLocationObjectName);

            String relativePathToOldLocation = oldLocationObjectName.substring(fromPath.length());
            String fullPathTillResourceNewLocation = toPath + relativePathToOldLocation;
            fullPathTillResourceNewLocations.add(fullPathTillResourceNewLocation);
        }
    }

    private List<String> createFoldersRecursively(List<String> fullPathTillFiles) throws ServerException,
            InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException, ErrorResponseException {
        StringBuilder pathToFolder = new StringBuilder();
        List<String> createdFolders = new ArrayList<>();
        try {
            for (String fullPathTillFile : fullPathTillFiles) {
                String[] partsTillFile = fullPathTillFile.split("/");
                pathToFolder.setLength(0);

                for (int i = 0; i < partsTillFile.length - 1; i++) {
                    if (partsTillFile[i].isEmpty()) {
                        continue;
                    }

                    pathToFolder.append(partsTillFile[i]).append("/");
                    String pathToFolderNormalized = String.valueOf(pathToFolder);
                    if (!doesPathExist(pathToFolderNormalized)) {
                        minioClient.putObject(
                                PutObjectArgs
                                        .builder()
                                        .bucket(minioBucketName)
                                        .object(pathToFolderNormalized)
                                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                                        .build()
                        );
                        createdFolders.add(pathToFolderNormalized);
                    }
                }
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            rollBackCreatedFolders(createdFolders);

            throw e;
        }
        return createdFolders;
    }

    private void rollBackCreatedFolders(List<String> createdFolders) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        for (int i = createdFolders.size() - 1; i >= 0; i--) {
            removeResource(createdFolders.get(i));
        }
    }

    private List<String> copyResource(List<String> fromPaths, List<String> toPath, List<String> copiedResources)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {
        for (int i = 0; i < toPath.size(); i++) {
            minioClient.copyObject(
                    CopyObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(toPath.get(i))
                            .source(
                                    CopySource
                                            .builder()
                                            .bucket(minioBucketName)
                                            .object(fromPaths.get(i))
                                            .build()
                            )
                            .build()
            );
            copiedResources.add(toPath.get(i));
        }
        return copiedResources;
    }

    private void rollBackCopiedResources(List<String> createdResources, List<String> createdFolders)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {
        for (int i = createdResources.size() - 1; i >= 0; i--) {
            removeResource(createdResources.get(i));
        }

        rollBackCreatedFolders(createdFolders);
    }

    private void removeResource(String fromPath) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        minioClient.removeObject(
                RemoveObjectArgs
                        .builder()
                        .bucket(minioBucketName)
                        .object(fromPath)
                        .build()
        );
    }

    private void removeResources(List<String> paths) throws ServerException,
            InsufficientDataException,
            ErrorResponseException,
            IOException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidResponseException,
            XmlParserException,
            InternalException {

        for (String path : paths) {
            try {
                removeResource(path);
            } catch (IOException firstIoEx) {
                try {
                    removeResource(path);
                } catch (IOException secondIoEx) {

                    if (doesPathExist(path)) {
                        throw secondIoEx;
                    }
                }
            }
        }
    }

    public boolean doesPathExist(String fullPath) {
        try {
            minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(fullPath)
                            .build()
            );

            return true;

        } catch (ErrorResponseException ere) {
            if ("NoSuchKey".equals(ere.errorResponse().code())) {
                return false;
            }
            throw new StorageException("Storage operation failed", ere);

        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public InputStream readData(String fullPathTillResource) {
        try {
            return minioClient.getObject(
                    GetObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(fullPathTillResource)
                            .build()
            );

        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    public void deleteFile(String fullPathTo) {
        deleteResources(Collections.singletonList(fullPathTo), new ArrayList<>());
    }

    public void deleteResources(List<String> filesPath, List<String> directoriesPath) {
        if (!filesPath.isEmpty()) {
            deleteResources(filesPath);
        }

        if (!directoriesPath.isEmpty()) {
            deleteResources(directoriesPath);
        }
    }

    private void deleteResources(List<String> resourcesPath) {
        List<DeleteObject> resources = resourcesPath.stream()
                .map(DeleteObject::new)
                .toList();

        try {
            List<DeleteError> initialErrorList = tryToRemove(resources);

            if (!initialErrorList.isEmpty()) {
                List<DeleteError> errorListAfterRepeat = tryToRemove(resources);

                if (!errorListAfterRepeat.isEmpty()) {
                    throw new ResourceDeletionException(
                            String.format(
                                    "Cannot delete resources after retry, the resource is: %s, the error is: %s",
                                    errorListAfterRepeat.stream().map(ErrorResponse::objectName).toList(),
                                    errorListAfterRepeat.stream().map(ErrorResponse::message).toList()
                            )
                    );
                }
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new StorageException("Storage operation failed", exception);
        }
    }

    private List<DeleteError> tryToRemove(List<DeleteObject> resources) throws ErrorResponseException, InsufficientDataException,
            InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .objects(resources)
                        .build()
        );

        List<DeleteError> errors = new ArrayList<>();

        for (Result<DeleteError> result : results) {
            errors.add(result.get());
        }

        return errors;
    }
}
