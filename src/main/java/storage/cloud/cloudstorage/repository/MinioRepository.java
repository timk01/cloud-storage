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

    public Iterable<Result<Item>> search(String query) {
        return minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(minioBucketName)
                        .prefix(query)
                        .recursive(true)
                        .build()
        );
    }
}