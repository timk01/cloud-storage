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
import storage.cloud.cloudstorage.service.resource.ResourceSearchService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceSearchServiceTest {

    @InjectMocks
    private ResourceSearchService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void searchIsSucceededWide() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        Item firstDirectory = new Contents("user-1-files/gorgon_root/");

        Item secondDirectory = new Contents("user-1-files/gorgon_root/gorgon_archive/");

        Item thirdDirectory = new Contents("user-1-files/gorgon_root/gorgon_archive/" +
                "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/");

        String pathTillFirstFile = "user-1-files/gorgon_root/gorgon_archive/" +
                "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/gorgon.jpg";
        Item firstFile = mock(Item.class);
        when(firstFile.objectName()).thenReturn(pathTillFirstFile);
        when(firstFile.size()).thenReturn(1500L);

        String pathTillSecondFile = "user-1-files/gorgon_root/gorgon_archive/" +
                "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/description_gorgon.txt";
        Item secondFile = mock(Item.class);
        when(secondFile.objectName()).thenReturn(pathTillSecondFile);
        when(secondFile.size()).thenReturn(123L);

        when(repository.search(minioRootFolder)).thenReturn(
                List.of(
                        firstDirectory,
                        secondDirectory,
                        thirdDirectory,
                        firstFile,
                        secondFile
                )
        );

        List<ResourceResponse> expected = List.of(
                ResourceResponse.builder()
                        .path("")
                        .name("gorgon_root")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path("gorgon_root/")
                        .name("gorgon_archive")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path("gorgon_root/gorgon_archive/")
                        .name("gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name("gorgon.jpg")
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name("description_gorgon.txt")
                        .size(123L)
                        .type(Type.FILE.name())
                        .build()
        );

        String query = "GoRgOn";
        List<ResourceResponse> actual = service.search(query, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).search(minioRootFolder);

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void searchIsSucceededButNothingMatchesQuery() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        Item firstDirectory = new Contents("user-1-files/gorgon_root/");

        String pathTillSecondFile = "user-1-files/gorgon_root/description_gorgon.txt";
        Item secondFile = mock(Item.class);
        when(secondFile.objectName()).thenReturn(pathTillSecondFile);

        when(repository.search(minioRootFolder)).thenReturn(
                List.of(
                        firstDirectory,
                        secondFile
                )
        );

        String query = "cat";
        List<ResourceResponse> actual = service.search(query, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).search(minioRootFolder);

        assertThat(actual).isEmpty();
    }
}
