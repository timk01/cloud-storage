package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.exception.managed.*;
import storage.cloud.cloudstorage.repository.ObjectStorage;
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
    private ObjectStorage storage;

    @Test
    public void moveFileIsSucceeded() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file1.txt";
        String pathTo = "folder9/folder10/test_file1.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);
        when(storage.doesPathExist(fullPathTo)).thenReturn(false);

        when(storage.retrieveResourceSize(fullPathFrom)).thenReturn(
                1500L
        );

        ResourceResponse expected = ResourceResponse.builder()
                .path("folder9/folder10/")
                .name("test_file1.txt")
                .size(1500L)
                .type(Type.FILE.name())
                .build();

        ResourceResponse actual = service.move(pathFrom, pathTo, userId);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, times(1)).doesPathExist(fullPathTo);
        verify(storage, times(1)).retrieveResourceSize(fullPathFrom);
        verify(storage, times(1)).moveFile(fullPathFrom, fullPathTo);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void moveDirectoryIsSucceeded() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);
        when(storage.doesPathExist(fullPathTo)).thenReturn(false);

        ResourceResponse expected = ResourceResponse.builder()
                .path("folder9/")
                .name("folder10")
                .type(Type.DIRECTORY.name())
                .build();

        ResourceResponse actual = service.move(pathFrom, pathTo, userId);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, times(1)).doesPathExist(fullPathTo);
        verify(storage, times(1)).moveDirectory(fullPathFrom, fullPathTo);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void moveResourceFailsSinceFromPathDoesNotExist() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(false);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, never()).doesPathExist(fullPathTo);
        verify(storage, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceFromPathAndToPathAreTheSame() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(SourceAndDestinationAreEqualException.class);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceFromAndToTypesAreDifferent() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/abc.txt";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(ResourceTypeMismatchException.class);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, never()).doesPathExist(fullPathTo);
        verify(storage, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveResourceFailsSinceToPathAlreadyExists() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String pathTo = "folder9/folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);
        when(storage.doesPathExist(fullPathTo)).thenReturn(true);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(DestinationResourceAlreadyExistsException.class);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, times(1)).doesPathExist(fullPathTo);
        verify(storage, never()).moveDirectory(fullPathFrom, fullPathTo);
    }

    @Test
    public void moveDirectoryFailsSinceDestinationIsItsOwnSubdirectory() {
        String pathFrom
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_.../";
        String pathTo = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_.../folder10/";
        String minioRootFolder = "user-1-files/";

        String fullPathFrom = minioRootFolder + pathFrom;
        String fullPathTo = minioRootFolder + pathTo;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathFrom)).thenReturn(true);
        when(storage.doesPathExist(fullPathTo)).thenReturn(false);

        assertThatThrownBy(() -> service.move(pathFrom, pathTo, userId))
                .isInstanceOf(ResourceMoveConflictException.class);

        verify(storage, times(1)).doesPathExist(fullPathFrom);
        verify(storage, times(1)).doesPathExist(fullPathTo);
        verify(storage, never()).moveDirectory(fullPathFrom, fullPathTo);
    }
}
