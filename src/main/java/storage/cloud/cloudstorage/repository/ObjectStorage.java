package storage.cloud.cloudstorage.repository;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface ObjectStorage {
    public List<StorageItem> retrieveDirectoryItems(String fullPath);

    public List<StorageItem> retrieveItemsRecursively(String fullPath);

    long retrieveResourceSize(String path);

    void creaTeFolder(String minioParentPath, String fullPath);

    void checkFiles(List<String> fullPaths);

    void upload(List<MultipartFile> files, List<String> fullPathTillFiles);

    void moveFile(String fromPath, String toPath);

    void moveDirectory(String fromPath, String toPath);

    boolean doesPathExist(String fullPath);

    InputStream readData(String fullPathTillResource);

    void deleteFile(String fullPathTo);

    void deleteResources(List<String> filesPath, List<String> directoriesPath);
}

