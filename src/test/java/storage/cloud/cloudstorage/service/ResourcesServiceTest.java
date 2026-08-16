package storage.cloud.cloudstorage.service;

import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Contents;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.InvalidFileNameException;
import storage.cloud.cloudstorage.exception.InvalidLoginDataException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.response.ResourceResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    public void uploadFileInfoIsSucceeded() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String fullPath = "user-1-files/" + parent;

        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();
        List<ResourceResponse> expected = List.of(
                ResourceResponse
                        .builder()
                        .path(parent)
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(gorgonType)
                        .build()
        );

        MultipartFile[] gorgonFile = new MultipartFile[] {new MockMultipartFile(
                "file",
                gorgonFilename,
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        )};

        String fullPathTillFile = "user-1-files/parent1/gorgon.jpg";

        List<ResourceResponse> actual = service.upload(
                parent,
                gorgonFile,
                userId
        );

        verify(storageInitializer, times(1)).initStorage(fullPath);
        verify(repository, times(1))
                .checkFiles(List.of(fullPathTillFile));
        verify(repository, times(1))
                .upload(List.of(gorgonFile[0]), List.of(fullPathTillFile));

        assertThat(actual).containsExactlyElementsOf(expected);
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

    @ParameterizedTest
    @MethodSource("emptyUploadData")
    public void uploadFileInfoWasNotDoneSinceOriginalFilenameIsEmpty(String gorgonFilename) throws Exception {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String fullPath = "user-1-files/" + parent;

        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];

        MultipartFile[] gorgonFile = new MultipartFile[] {new MockMultipartFile(
                "file",
                gorgonFilename,
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        )};

        List<ResourceResponse> actual = service.upload(
                parent,
                gorgonFile,
                userId
        );

        verify(storageInitializer, times(1)).initStorage(fullPath);
        verify(repository, times(1))
                .checkFiles(List.of());
        verify(repository, times(1))
                .upload(List.of(), List.of());

        assertThat(actual).isEmpty();
    }

    private static Stream<Arguments> emptyUploadData() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of((String) null)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidFileUploadData")
    public void uploadFileInfoWasNotDoneSinceOriginalFileNameIsInvalid(String gorgonFilename) throws Exception {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String fullPath = "user-1-files/" + parent;

        Long userId = 1L;

        MultipartFile[] gorgonFile = new MultipartFile[] {new MockMultipartFile(
                "file",
                gorgonFilename,
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        )};

        assertThatThrownBy(() -> service.upload(parent, gorgonFile, userId))
                .isInstanceOf(InvalidFileNameException.class);

        verify(storageInitializer, times(1)).initStorage(fullPath);
        verify(repository, never())
                .checkFiles(anyList());
        verify(repository, never()).upload(anyList(), anyList());
    }

    private static Stream<Arguments> invalidFileUploadData() {
        return Stream.of(
                Arguments.of(".."),
                Arguments.of(".")
        );
    }
}