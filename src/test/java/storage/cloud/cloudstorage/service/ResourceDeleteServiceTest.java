package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.exception.managed.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
import storage.cloud.cloudstorage.service.resource.ResourceDeleteService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceDeleteServiceTest {

    @InjectMocks
    private ResourceDeleteService service;

    @Mock
    private ObjectStorage storage;

    @Test
    public void deleteFileIsSucceeded() {
        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file1.txt";
        String minioRootFolder = "user-1-files/";

        String fullPath = minioRootFolder + path;

        Long userId = 1L;

        when(storage.doesPathExist(fullPath)).thenReturn(true);

        service.delete(path, userId);

        verify(storage, times(1)).doesPathExist(fullPath);
        verify(storage, times(1)).deleteFile(fullPath);
    }

    @Test
    public void deleteFolderIsSucceeded() {
        String path = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        StorageItem rootFolderMarker = new StorageItem(
                "user-1-files/folder1/folder2/folder3/",
                true,
                0L
        );

        StorageItem file1 = new StorageItem(
                "user-1-files/folder1/folder2/folder3/gorgon.jpg",
                false,
                10L
        );

        StorageItem nestedFolder = new StorageItem(
                "user-1-files/folder1/folder2/folder3/newFolder/",
                true,
                0L
        );

        StorageItem file2 = new StorageItem(
                "user-1-files/folder1/folder2/folder3/newFolder/file2.txt",
                false,
                10L
        );

        StorageItem file3InNewFolder = new StorageItem(
                "user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt",
                false,
                10L
        );

        when(storage.retrieveItemsRecursively(fullPathToResource))
                .thenReturn(List.of(
                        rootFolderMarker,
                        file1,
                        nestedFolder,
                        file2,
                        file3InNewFolder
                ));

        List<String> filesPath = List.of(
                file1.resourcePath(),
                file2.resourcePath(),
                file3InNewFolder.resourcePath()
        );

        List<String> directoriesPath = List.of(
                nestedFolder.resourcePath(),
                rootFolderMarker.resourcePath()
        );


        service.delete(path, userId);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, times(1)).retrieveItemsRecursively(fullPathToResource);
        verify(storage, times(1)).deleteResources(filesPath, directoriesPath);
    }

    @Test
    public void deleteEmptyFolderIsSucceeded() {
        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        StorageItem rootFolderMarker = new StorageItem(
                "user-1-files/folder1/folder2/folder3/",
                true,
                0L
        );

        when(storage.retrieveItemsRecursively(fullPathToResource))
                .thenReturn(List.of(
                        rootFolderMarker
                ));

        List<String> filesPath = List.of(
        );

        List<String> directoriesPath = List.of(
                rootFolderMarker.resourcePath()
        );

        service.delete(path, userId);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, times(1)).retrieveItemsRecursively(fullPathToResource);
        verify(storage, times(1)).deleteResources(filesPath, directoriesPath);
    }

    @Test
    public void deleteFolderIsFailedDueToNoResourceFound() {
        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveItemsRecursively(anyString());
        verify(storage, never()).deleteFile(anyString());
        verify(storage, never()).deleteResources(anyList(), anyList());
    }
}
