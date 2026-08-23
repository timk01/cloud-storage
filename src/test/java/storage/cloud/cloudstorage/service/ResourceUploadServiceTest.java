package storage.cloud.cloudstorage.service;

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
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.resource.ResourceUploadService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceUploadServiceTest {

    @InjectMocks
    private ResourceUploadService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void uploadFileInfoIsSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String minioRootFolder = "user-1-files/";

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

        MultipartFile[] gorgonFile = new MultipartFile[]{new MockMultipartFile(
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

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1))
                .checkFiles(List.of(fullPathTillFile));
        verify(repository, times(1))
                .upload(List.of(gorgonFile[0]), List.of(fullPathTillFile));

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @ParameterizedTest
    @MethodSource("emptyUploadData")
    public void uploadFileInfoWasNotDoneSinceOriginalFilenameIsEmpty(String gorgonFilename) {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];

        MultipartFile[] gorgonFile = new MultipartFile[]{new MockMultipartFile(
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

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
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
    public void uploadFileInfoWasNotDoneSinceOriginalFileNameIsInvalid(String gorgonFilename) {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String parent = "parent1/";
        String minioRootFolder = "user-1-files/";

        Long userId = 1L;

        MultipartFile[] gorgonFile = new MultipartFile[]{new MockMultipartFile(
                "file",
                gorgonFilename,
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        )};

        assertThatThrownBy(() -> service.upload(parent, gorgonFile, userId))
                .isInstanceOf(InvalidFileNameException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
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