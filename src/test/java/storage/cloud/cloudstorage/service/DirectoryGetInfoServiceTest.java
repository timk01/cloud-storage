package storage.cloud.cloudstorage.service;

import io.minio.messages.Contents;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
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
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    /**
     * Особное внимание на currentParent - в результаты он не попадает, т.к. мы ищем ресурсы в самой папочке,
     * а не на уровне выше
     */
    @Test
    public void getFolderInfoIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String minioRootFolder = "user-1-files/";
        String fullPath = minioRootFolder + parent;

        Long userId = 1L;

        Item currentParent = mock(Item.class);
        when(currentParent.objectName()).thenReturn(fullPath);

        String child1 = "child1";
        Item firstFolder = new Contents("user-1-files/parent1/child1/");

        String child2 = "child2";
        Item secondFolder = new Contents("user-1-files/parent1/child2/");

        String file = "gorgon.jpg";
        String pathTillFile = "user-1-files/parent1/gorgon.jpg";
        Item fileDownloaded = mock(Item.class);
        when(fileDownloaded.objectName()).thenReturn(pathTillFile);
        when(fileDownloaded.isDir()).thenReturn(false);
        when(fileDownloaded.size()).thenReturn(1500L);

        when(repository.getFolderInfo(fullPath)).thenReturn(
                List.of(
                        currentParent,
                        firstFolder,
                        secondFolder,
                        fileDownloaded
                )
        );

        List<ResourceResponse> expected = List.of(
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name(child1)
                        .type(Type.DIRECTORY.name())
                        .build(),
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name(child2)
                        .type(Type.DIRECTORY.name())
                        .build(),
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name(file)
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build()
        );
        List<ResourceResponse> actual = service.getFolderInfo(parent, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).getFolderInfo(fullPath);

        assertThat(actual).containsExactlyElementsOf(expected);
    }
}
