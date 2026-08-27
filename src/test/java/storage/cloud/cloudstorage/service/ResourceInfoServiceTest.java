package storage.cloud.cloudstorage.service;

import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.exception.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
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
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void gettingResourceInfoForFileIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/folder2/folder3/gorgon.jpg";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        StatObjectResponse firstFileStatObject = mock(StatObjectResponse.class);
        when(firstFileStatObject.size()).thenReturn(1500L);

        when(repository.getObjectResponse(fullPathToResource)).thenReturn(
                firstFileStatObject
        );

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("folder1/folder2/folder3/");
        assertThat(actual.name()).isEqualTo("gorgon.jpg");
        assertThat(actual.size()).isEqualTo(1500L);
        assertThat(actual.type()).isEqualTo(Type.FILE.name());

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, times(1)).getObjectResponse(fullPathToResource);
    }

    @Test
    public void gettingResourceInfoForFolderIsSucceeded() {
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

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("folder1/folder2/");
        assertThat(actual.name()).isEqualTo("folder3");
        assertThat(actual.type()).isEqualTo(Type.DIRECTORY.name());

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).getObjectResponse(anyString());
    }

    @Test
    public void gettingResourceInfoForRootFolderIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        ResourceResponse actual = service.resourceInfo(path, userId);

        assertThat(actual.path()).isEqualTo("");
        assertThat(actual.name()).isEqualTo("folder1");
        assertThat(actual.type()).isEqualTo(Type.DIRECTORY.name());

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).getObjectResponse(anyString());
    }

    @Test
    public void gettingResourceInfoForFileFailsDueToPathDoesNotExist() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "folder1/folder2/folder3/abrakadabra";
        String minioRootFolder = "user-1-files/";
        String fullPathToResource = minioRootFolder + path;
        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.resourceInfo(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).getObjectResponse(anyString());
    }

    @Test
    public void gettingResourceInfoForFolderFailsDueToPathDoesNotExist() {
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

        assertThatThrownBy(() -> service.resourceInfo(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).getObjectResponse(anyString());
    }
}
