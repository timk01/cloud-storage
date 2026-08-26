package storage.cloud.cloudstorage.service;

import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.exception.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.service.resource.ResourceDeleteService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceDeleteServiceTest {

    @InjectMocks
    private ResourceDeleteService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void deleteFileIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file1.txt";
        String minioRootFolder = "user-1-files/";

        String fullPath = minioRootFolder + path;

        Long userId = 1L;

        when(repository.doesPathExist(fullPath)).thenReturn(true);

        service.delete(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPath);
        verify(repository, times(1)).deleteFile(fullPath);
    }

    @Test
    public void deleteFolderIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        Item rootFolderMarker = mock(Item.class);
        when(rootFolderMarker.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/");

        Item file1 = mock(Item.class);
        when(file1.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/gorgon.jpg");

        Item nestedFolder = mock(Item.class);
        when(nestedFolder.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/newFolder/");

        Item file2 = mock(Item.class);
        when(file2.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/newFolder/file2.txt");

        Item file3InNewFolder = mock(Item.class);
        when(file3InNewFolder.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt");

        when(repository.search(fullPathToResource)).thenReturn(
                List.of(
                        rootFolderMarker,
                        file1,
                        nestedFolder,
                        file2,
                        file3InNewFolder
                )
        );

        List<String> filesPath = List.of(
                file1.objectName(),
                file2.objectName(),
                file3InNewFolder.objectName()
        );

        List<String> directoriesPath = List.of(
                nestedFolder.objectName(),
                rootFolderMarker.objectName()
        );


        service.delete(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, times(1)).search(fullPathToResource);
        verify(repository, times(1)).deleteResources(filesPath, directoriesPath);
    }

    @Test
    public void deleteEmptyFolderIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        Item rootFolderMarker = mock(Item.class);
        when(rootFolderMarker.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/");

        when(repository.search(fullPathToResource)).thenReturn(
                List.of(
                        rootFolderMarker
                )
        );

        List<String> filesPath = List.of(
        );

        List<String> directoriesPath = List.of(
                rootFolderMarker.objectName()
        );

        service.delete(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, times(1)).search(fullPathToResource);
        verify(repository, times(1)).deleteResources(filesPath, directoriesPath);
    }

    @Test
    public void deleteFolderIsFailedDueToNoResourceFound() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).search(anyString());
        verify(repository, never()).deleteFile(anyString());
        verify(repository, never()).deleteResources(anyList(), anyList());
    }
}
