package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import storage.cloud.cloudstorage.exception.FolderAlreadyExistsException;
import storage.cloud.cloudstorage.exception.FolderNotFoundException;
import storage.cloud.cloudstorage.exception.ParentFolderHasNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@Repository
public class MinioRepository {

    private final MinioClient minioClient;
    private final String minioBucketName;

    public void creaTeFolder(String parentFolder, String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        checkParentFolder(parentFolder);

        checkCurrentFolder(fullPath);

        minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(minioBucketName)
                        .object(fullPath)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build()
        );

        //toDo посмотреть для остальных ручек (где файлы), тут сайз есть

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
                    ));
        }
    }

    private void checkCurrentFolder(String fullPath) {
        Iterable<Result<Item>> folderResults = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(fullPath)
                        .maxKeys(1)
                        .build()
        );

        checkFolderDoesNotExist(fullPath, folderResults);
    }

    private void checkFolderDoesNotExist(String fullPath, Iterable<Result<Item>> folderResults) {
        boolean hasFolder = folderResults.iterator().hasNext();

        if (hasFolder) {
            throw new FolderAlreadyExistsException(
                    String.format(
                            "Folder already exist: %s ", fullPath
                    ));
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
}