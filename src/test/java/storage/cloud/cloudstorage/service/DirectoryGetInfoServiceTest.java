package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryGetInfoService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectoryGetInfoServiceTest {

    @InjectMocks
    private DirectoryGetInfoService service;

    @Mock
    private ObjectStorage storage;

    @Test
    public void getFolderInfoIsSucceeded() {
        String parent = "parent1/";
        String minioRootFolder = "user-1-files/";
        String fullPath = minioRootFolder + parent;

        Long userId = 1L;

        StorageItem currentParent = new StorageItem(
                fullPath,
                true,
                0L
        );

        StorageItem firstFolder = new StorageItem(
                "user-1-files/parent1/child1/",
                true,
                0L
        );

        StorageItem secondFolder = new StorageItem(
                "user-1-files/parent1/child2/",
                true,
                0L
        );

        StorageItem fileItem = new StorageItem(
                "user-1-files/parent1/gorgon.jpg",
                false,
                1500L
        );

        when(storage.retrieveDirectoryItems(fullPath))
                .thenReturn(List.of(
                        currentParent,
                        firstFolder,
                        secondFolder,
                        fileItem
                ));

        List<ResourceResponse> expected = List.of(
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name("child1")
                        .type(Type.DIRECTORY.name())
                        .build(),
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name("child2")
                        .type(Type.DIRECTORY.name())
                        .build(),
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name("gorgon.jpg")
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build()
        );
        List<ResourceResponse> actual = service.getFolderInfo(parent, userId);

        verify(storage, times(1)).retrieveDirectoryItems(fullPath);

        assertThat(actual).containsExactlyElementsOf(expected);
    }
}
