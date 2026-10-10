package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryCreateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DirectoryCreateServiceTest {

    @InjectMocks
    private DirectoryCreateService service;

    @Mock
    private ObjectStorage storage;

    @Test
    public void createStorageIsSucceeded() {
        String cleanPath = "folder_dd1503f9-49ec-4b9c-931c-4a088bba7bc7";
        String pathAsDirectory = cleanPath + "/";
        String minioRootFolder = "user-1-files/";
        String fullPath = minioRootFolder + pathAsDirectory;
        String responseParentPath = "";
        Long userId = 1L;

        ResourceResponse expected = ResourceResponse
                .builder()
                .path(responseParentPath)
                .name(cleanPath)
                .type(Type.DIRECTORY.name())
                .build();
        ResourceResponse actual = service.createFolder(pathAsDirectory, userId);

        verify(storage, times(1)).creaTeFolder(minioRootFolder, fullPath);

        assertThat(actual.path()).isEqualTo(expected.path());
        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.type()).isEqualTo(expected.type());
    }
}
