package storage.cloud.cloudstorage.service;

import io.minio.Result;
import io.minio.errors.MinioException;
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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourcesServiceTest {

    @InjectMocks
    private ResourcesService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void createStorageIsSucceeded() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

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

        verify(storageInitializer, times(1)).initStorage(fullPath);
        verify(repository, times(1)).creaTeFolder(minioRootFolder, fullPath);

        assertThat(actual.path()).isEqualTo(expected.path());
        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.type()).isEqualTo(expected.type());
    }

    /**
     * Особное внимание на currentParent - в результаты он не попадает, т.к. мы ищем ресурсы в самой папочке,
     * а не на уровне выше
     */
    @Test
    public void getStorageInfoIsSucceeded() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String fullPath = "user-1-files/" + parent;

        Long userId = 1L;

        Item currentParent = mock(Item.class);
        when(currentParent.objectName()).thenReturn(fullPath);
        Result<Item> parentFolder = new Result<>(currentParent);

        String child1 = "child1";
        Item firstFolder = new Contents("user-1-files/parent1/child1/");
        Result<Item> firstFolderResult = new Result<>(firstFolder);

        String child2 = "child2";
        Item secondFolder = new Contents("user-1-files/parent1/child2/");
        Result<Item> secondFolderResult = new Result<>(secondFolder);

        String file = "gorgon.jpg";
        String pathTillFile = "user-1-files/parent1/gorgon.jpg";
        Item fileDownloaded = mock(Item.class);
        when(fileDownloaded.objectName()).thenReturn(pathTillFile);
        when(fileDownloaded.isDir()).thenReturn(false);
        when(fileDownloaded.size()).thenReturn(1500L);
        Result<Item> fileResult = new Result<>(fileDownloaded);

        when(repository.getFolderInfo(fullPath)).thenReturn(
                List.of(
                        parentFolder,
                        firstFolderResult,
                        secondFolderResult,
                        fileResult
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

        verify(storageInitializer, times(1)).initStorage(fullPath);
        verify(repository, times(1)).getFolderInfo(fullPath);

        assertThat(actual).containsExactlyElementsOf(expected);
    }
}