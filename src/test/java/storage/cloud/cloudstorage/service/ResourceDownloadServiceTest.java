package storage.cloud.cloudstorage.service;

import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import storage.cloud.cloudstorage.exception.managed.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.service.resource.ResourceDownloadService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceDownloadServiceTest {

    @InjectMocks
    private ResourceDownloadService service;

    @Mock
    private MinioRepository repository;

    @Mock
    private StorageInitializer storageInitializer;

    @Test
    public void prepareFileSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        List<ResourceDownloadService.PreparedFileRecord> expectedRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "file2.txt",
                        "user-1-files/gorgon_root/gorgon_archive/" +
                                "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt",
                        "FILE"
                )
        );

        List<ResourceDownloadService.PreparedFileRecord> actualRecords = service.prepareResource(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).search(fullPathToResource);

        assertThat(actualRecords).isEqualTo(expectedRecords);
    }

    @Test
    public void prepareFolderSucceeded() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        Item rootFolderMarker = mock(Item.class);
        when(rootFolderMarker.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/");

        Item file1 = mock(Item.class);
        when(file1.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/gorgon.jpg");

        Item emptyFolder = mock(Item.class);
        when(emptyFolder.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/newFolder/");

        Item file2 = mock(Item.class);
        when(file2.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/newFolder/file2.txt");

        Item file3InNewFolder = mock(Item.class);
        when(file3InNewFolder.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt");

        when(repository.search(fullPathToResource)).thenReturn(
                List.of(
                        rootFolderMarker,
                        file1,
                        emptyFolder,
                        file2,
                        file3InNewFolder
                )
        );

        List<ResourceDownloadService.PreparedFileRecord> expectedRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "gorgon.jpg",
                        "user-1-files/folder1/folder2/folder3/gorgon.jpg",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/",
                        "user-1-files/folder1/folder2/folder3/newFolder/",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/file2.txt",
                        "user-1-files/folder1/folder2/folder3/newFolder/file2.txt",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "folder4/folder5/b.txt",
                        "user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt",
                        "DIRECTORY"
                )
        );

        List<ResourceDownloadService.PreparedFileRecord> actualRecords = service.prepareResource(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, times(1)).search(fullPathToResource);

        assertThat(actualRecords).containsExactlyElementsOf(expectedRecords);
    }

    @Test
    public void prepareEmptyFolderSucceededWithNoRecordsToReturn() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(true);

        Item rootFolderMarker = mock(Item.class);
        when(rootFolderMarker.objectName()).thenReturn("user-1-files/folder1/folder2/folder3/");

        when(repository.search(fullPathToResource)).thenReturn(
                List.of(
                        rootFolderMarker
                )
        );

        List<ResourceDownloadService.PreparedFileRecord> actualRecords = service.prepareResource(path, userId);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, times(1)).search(fullPathToResource);

        assertThat(actualRecords).isEmpty();
    }

    @Test
    public void prepareResourceIsFailedDueToNoResourceFound() {
        ReflectionTestUtils.setField(
                service,
                "minioBucketName",
                "user-files"
        );

        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(repository.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.prepareResource(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storageInitializer, times(1)).initStorage(minioRootFolder);
        verify(repository, times(1)).doesPathExist(fullPathToResource);
        verify(repository, never()).search(fullPathToResource);
    }

    @Test
    public void downloadFileSucceeded() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        List<ResourceDownloadService.PreparedFileRecord> preparedFileRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "file2.txt",
                        "user-1-files/gorgon_root/gorgon_archive/" +
                                "gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt",
                        "FILE"
                )
        );

        String fake_data
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt";

        InputStream inputStream = new ByteArrayInputStream(fake_data.getBytes((StandardCharsets.UTF_8)));

        when(repository.readData(preparedFileRecords.get(0).fullPathTillResource())).thenReturn(inputStream);


        service.download(preparedFileRecords, outputStream, fake_data);


        verify(repository, times(1)).readData(preparedFileRecords.get(0).fullPathTillResource());

        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    public void downloadFolderSucceeded() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        List<ResourceDownloadService.PreparedFileRecord> preparedRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "gorgon.jpg",
                        "user-1-files/folder1/folder2/folder3/gorgon.jpg",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/",
                        "user-1-files/folder1/folder2/folder3/newFolder/",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/file2.txt",
                        "user-1-files/folder1/folder2/folder3/newFolder/file2.txt",
                        "DIRECTORY"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "folder4/folder5/b.txt",
                        "user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt",
                        "DIRECTORY"
                )
        );

        String fake_data
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt"
                + "_"
                + UUID.randomUUID();

        for (ResourceDownloadService.PreparedFileRecord preparedRecord : preparedRecords) {
            InputStream inputStream;
            if (preparedRecord.fullPathTillResource().endsWith("/")) {
                inputStream = new ByteArrayInputStream(new byte[0]);
            } else {
                inputStream = new ByteArrayInputStream(fake_data.getBytes((StandardCharsets.UTF_8)));
            }

            when(repository.readData(preparedRecord.fullPathTillResource())).thenReturn(inputStream);
        }

        service.download(preparedRecords, outputStream, "folder1/folder2/folder3/");

        for (ResourceDownloadService.PreparedFileRecord preparedRecord : preparedRecords) {
            verify(repository, times(1)).readData(preparedRecord.fullPathTillResource());
        }

        assertThat(outputStream.size()).isGreaterThan(0);
    }
}
