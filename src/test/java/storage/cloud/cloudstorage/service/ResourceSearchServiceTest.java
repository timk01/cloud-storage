package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
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
    private ObjectStorage storage;

    @Mock
    private MinioProperties properties;

    @Test
    public void searchIsSucceededWide() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        StorageItem firstDirectory = new StorageItem(
                "user-1-files/gorgon_root/",
                true,
                0L
        );

        StorageItem secondDirectory = new StorageItem(
                "user-1-files/gorgon_root/gorgon_archive/",
                true,
                0L
        );

        StorageItem thirdDirectory = new StorageItem(
                "user-1-files/gorgon_root/gorgon_archive/" +
                        "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/",
                true,
                0L
        );

        String pathTillFirstFile =
                "user-1-files/gorgon_root/gorgon_archive/" +
                        "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/gorgon.jpg";

        StorageItem firstFile = new StorageItem(
                pathTillFirstFile,
                false,
                1500L
        );

        String pathTillSecondFile =
                "user-1-files/gorgon_root/gorgon_archive/" +
                        "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/description_gorgon.txt";

        StorageItem secondFile = new StorageItem(
                pathTillSecondFile,
                false,
                123L
        );

        when(storage.retrieveItemsRecursively(minioRootFolder))
                .thenReturn(List.of(
                        firstDirectory,
                        secondDirectory,
                        thirdDirectory,
                        firstFile,
                        secondFile
                ));

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

        verify(storage, times(1)).retrieveItemsRecursively(minioRootFolder);

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    public void searchIsSucceededButNothingMatchesQuery() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        StorageItem firstDirectory = new StorageItem(
                "user-1-files/gorgon_root/",
                true,
                0L
        );

        String pathTillSecondFile = "user-1-files/gorgon_root/description_gorgon.txt";

        StorageItem secondFile = new StorageItem(
                pathTillSecondFile,
                false,
                123L
        );

        when(storage.retrieveItemsRecursively(minioRootFolder))
                .thenReturn(List.of(
                        firstDirectory,
                        secondFile
                ));

        String query = "cat";
        List<ResourceResponse> actual = service.search(query, userId);

        verify(storage, times(1)).retrieveItemsRecursively(minioRootFolder);

        assertThat(actual).isEmpty();
    }
}
