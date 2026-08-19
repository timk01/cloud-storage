package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.FileAlreadyExistsException;
import storage.cloud.cloudstorage.exception.FolderAlreadyExistsException;
import storage.cloud.cloudstorage.exception.FolderNotFoundException;
import storage.cloud.cloudstorage.exception.ParentFolderHasNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class MinioRepository {

    private final MinioClient minioClient;
    private final String minioBucketName;

    public void creaTeFolder(String minioParentPath, String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        checkParentFolder(minioParentPath);

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

    public boolean doesPathExist(String fullPath) throws ServerException, InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ErrorResponseException {
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
            throw ere;
        }
    }

    public Iterable<Result<Item>> getFolderInfo(String fullPath) {
        Iterable<Result<Item>> folderResults = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(fullPath)
                        .recursive(false)
                        .build()
        );

        checkFolderExists(fullPath, folderResults);

        return folderResults;
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

    /**
     * если файл ЕСТЬ в батче (т.е. нашли что любой из файлов уже был загружен) - значит эксепшнион
     *
     * @param fullPaths
     * @throws ServerException
     * @throws InsufficientDataException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidResponseException
     * @throws XmlParserException
     * @throws InternalException
     */

    public void checkFiles(List<String> fullPaths) throws ServerException, InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ErrorResponseException {
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
    }

    public void upload(List<MultipartFile> files, List<String> fullPathTillFiles) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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
    }

    public Iterable<Result<Item>> search(String preparedRoot) {
        return minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(preparedRoot)
                        .recursive(true)
                        .build()
        );
    }

    public void moveFile(String fromPath, String toPath) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        createFoldersRecursively(List.of(toPath));

        copyFile(fromPath, toPath);

        removeFile(fromPath);
    }

    private void createFoldersRecursively(List<String> fullPathTillFiles) throws ServerException, InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ErrorResponseException {
        StringBuilder pathToFolder = new StringBuilder();
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
                }
            }
        }
    }

    private void copyFile(String fromPath, String toPath) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.copyObject(
                CopyObjectArgs
                        .builder()
                        .bucket(minioBucketName)
                        .object(toPath)
                        .source(
                                CopySource
                                        .builder()
                                        .bucket(minioBucketName)
                                        .object(fromPath)
                                        .build()
                        )
                        .build()
        );
    }

    private void removeFile(String fromPath) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.removeObject(
                RemoveObjectArgs
                        .builder()
                        .bucket(minioBucketName)
                        .object(fromPath)
                        .build()
        );
    }

    public StatObjectResponse getObjectResponse (String pathTillObject) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.statObject(
                StatObjectArgs
                        .builder()
                        .bucket(minioBucketName)
                        .object(pathTillObject)
                        .build()
        );
    }
}