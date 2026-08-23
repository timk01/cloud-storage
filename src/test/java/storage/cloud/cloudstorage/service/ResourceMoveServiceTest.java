package storage.cloud.cloudstorage.service;

import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.exception.*;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.resource.ResourceMoveService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceMoveServiceTest {

    @InjectMocks
    private ResourceMoveService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void moveFileIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file1.txt";
        String pathTo = "folder9/folder10/test_file1.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);
        when(repository.doesPathExist(fullPathTo)).thenReturn(false);

        StatObjectResponse firstFileStatObject = mock(StatObjectResponse.class);
        when(firstFileStatObject.size()).thenReturn(1500L);

        when(repository.getObjectResponse(fullPathFrom)).thenReturn(firstFileStatObject);

        ResourceResponse expected = ResourceResponse.builder()
                .path("folder9/folder10/")
                .name("test_file1.txt")
                .size(firstFileStatObject.size())
                .type(Type.FILE.name())
                .build();

        ResourceResponse actual = service.move(pathFrom, pathTo, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, times(1)).doesPathExist(fullPathTo);
        verify(repository, times(1)).moveFile(fullPathFrom, fullPathTo);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void moveDirectoryIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);
        when(repository.doesPathExist(fullPathTo)).thenReturn(false);

        ResourceResponse expected = ResourceResponse.builder()
                .path("folder9/")
                .name("folder10")
                .type(Type.DIRECTORY.name())
                .build();

        ResourceResponse actual = service.move(pathFrom, pathTo, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, times(1)).doesPathExist(fullPathTo);
        verify(repository, times(1)).moveDirectory(fullPathFrom, fullPathTo);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void moveResourceFailsSinceFromPathDoesNotExist() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(false);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, never()).doesPathExist(fullPathTo);
        verify(repository, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceFromPathAndToPathAreTheSame() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(SourceAndDestinationAreEqualException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceFromAndToTypesAreDifferent() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/abc.txt";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(ResourceTypeMismatchException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, never()).doesPathExist(fullPathTo);
        verify(repository, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceToPathAlreadyExists() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);
        when(repository.doesPathExist(fullPathTo)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(DestinationResourceAlreadyExistsException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, times(1)).doesPathExist(fullPathTo);
        verify(repository, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveDirectoryFailsSinceDestinationIsItsOwnSubdirectory() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_.../";
        String pathTo = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_.../folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathFrom)).thenReturn(true);
        when(repository.doesPathExist(fullPathTo)).thenReturn(false);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(ResourceMoveConflictException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathFrom);
        verify(repository, times(1)).doesPathExist(fullPathTo);
        verify(repository, never()).moveDirectory(fullPathFrom, fullPathTo);
    }
}
