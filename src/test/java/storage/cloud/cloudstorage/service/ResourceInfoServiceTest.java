package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.exception.managed.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.resource.ResourceInfoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceInfoServiceTest {

    @InjectMocks
    private ResourceInfoService service;

    @Mock
    private ObjectStorage storage;

    @Mock
    private MinioProperties properties;

    @Test
    public void gettingResourceInfoForFileIsSucceeded() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "folder1/folder2/folder3/gorgon.jpg";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        when(storage.retrieveResourceSize(fullPathToResource)).thenReturn(
                1500L
        );

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("folder1/folder2/folder3/");
        assertThat(actual.name()).isEqualTo("gorgon.jpg");
        assertThat(actual.size()).isEqualTo(1500L);
        assertThat(actual.type()).isEqualTo(Type.FILE.name());

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, times(1)).retrieveResourceSize(fullPathToResource);
    }

    @Test
    public void gettingResourceInfoForFolderIsSucceeded() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("folder1/folder2/");
        assertThat(actual.name()).isEqualTo("folder3");
        assertThat(actual.type()).isEqualTo(Type.DIRECTORY.name());

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveResourceSize(anyString());
    }

    @Test
    public void gettingResourceInfoForRootFolderIsSucceeded() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "folder1/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("");
        assertThat(actual.name()).isEqualTo("folder1");
        assertThat(actual.type()).isEqualTo(Type.DIRECTORY.name());

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveResourceSize(anyString());
    }

    @Test
    public void gettingResourceInfoForFileFailsDueToPathDoesNotExist() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "folder1/folder2/folder3/abrakadabra";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.resourceInfo(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveResourceSize(anyString());
    }

    @Test
    public void gettingResourceInfoForFolderFailsDueToPathDoesNotExist() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.resourceInfo(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveResourceSize(anyString());
    }
}
