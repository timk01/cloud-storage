package storage.cloud.cloudstorage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.FolderAlreadyExistsException;
import storage.cloud.cloudstorage.exception.FolderNotFoundException;
import storage.cloud.cloudstorage.exception.ParentFolderHasNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

    public void checkFile(String fullPath) throws ServerException, InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {
            minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(minioBucketName)
                            .object(fullPath)
                            .build()
            );
        } catch (ErrorResponseException  e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                //409 - файл уже существует
            }
        }
    }

    public List<ObjectWriteResponse> upload(List<MultipartFile> files, List<String> cleanPaths) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<ObjectWriteResponse> filesAdded = new ArrayList<>();
        try {
            int index = 0;
            for (MultipartFile file : files) {
                //подумать... про вариант когда файлов записалось, а потом ошибка ?

                if (index >= cleanPaths.size()) {
                    break;
                }

                ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                        PutObjectArgs
                                .builder()
                                .bucket(minioBucketName)
                                .object(cleanPaths.get(index++))
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());

                filesAdded.add(objectWriteResponse);
            }
        } catch (ServerException e) {
            //ignored ?
        }

        return filesAdded;
    }
}