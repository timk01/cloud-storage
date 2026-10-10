package storage.cloud.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import storage.cloud.cloudstorage.config.MinioProperties;
import storage.cloud.cloudstorage.exception.managed.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.repository.ObjectStorage;
import storage.cloud.cloudstorage.repository.StorageItem;
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
    private ObjectStorage storage;

    @Mock
    private MinioProperties properties;

    @Test
    public void prepareFileSucceeded() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

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

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveItemsRecursively(fullPathToResource);

        assertThat(actualRecords).isEqualTo(expectedRecords);
    }

    @Test
    public void prepareFolderSucceeded() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        StorageItem rootFolderMarker = new StorageItem(
                "user-1-files/folder1/folder2/folder3/",
                true,
                0L
        );

        StorageItem file1 = new StorageItem(
                "user-1-files/folder1/folder2/folder3/gorgon.jpg",
                false,
                10L
        );

        StorageItem nestedFolder = new StorageItem(
                "user-1-files/folder1/folder2/folder3/newFolder/",
                true,
                0L
        );

        StorageItem file2 = new StorageItem(
                "user-1-files/folder1/folder2/folder3/newFolder/file2.txt",
                false,
                10L
        );

        StorageItem file3InNewFolder = new StorageItem(
                "user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt",
                false,
                10L
        );

        when(storage.retrieveItemsRecursively(fullPathToResource))
                .thenReturn(List.of(
                        rootFolderMarker,
                        file1,
                        nestedFolder,
                        file2,
                        file3InNewFolder
                ));

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

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, times(1)).retrieveItemsRecursively(fullPathToResource);

        assertThat(actualRecords).containsExactlyElementsOf(expectedRecords);
    }

    @Test
    public void prepareEmptyFolderSucceededWithNoRecordsToReturn() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path = "folder1/folder2/folder3/";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(true);

        StorageItem rootFolderMarker = new StorageItem(
                "user-1-files/folder1/folder2/folder3/",
                true,
                0L
        );

        when(storage.retrieveItemsRecursively(fullPathToResource))
                .thenReturn(List.of(
                        rootFolderMarker
                ));

        List<ResourceDownloadService.PreparedFileRecord> actualRecords = service.prepareResource(path, userId);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, times(1)).retrieveItemsRecursively(fullPathToResource);

        assertThat(actualRecords).isEmpty();
    }

    @Test
    public void prepareResourceIsFailedDueToNoResourceFound() {
        when(properties.bucket())
                .thenReturn(new MinioProperties.Bucket("user-files"));

        String path
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/file2.txt";
        String minioRootFolder = "user-1-files/";

        String fullPathToResource = minioRootFolder + path;

        Long userId = 1L;

        when(storage.doesPathExist(fullPathToResource)).thenReturn(false);

        assertThatThrownBy(() -> service.prepareResource(path, userId))
                .isInstanceOf(SourceResourceNotFoundException.class);

        verify(storage, times(1)).doesPathExist(fullPathToResource);
        verify(storage, never()).retrieveItemsRecursively(fullPathToResource);
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

        when(storage.readData(preparedFileRecords.get(0).fullPathTillResource())).thenReturn(inputStream);


        service.download(preparedFileRecords, outputStream, fake_data);

        verify(storage, times(1)).readData(preparedFileRecords.get(0).fullPathTillResource());

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

            when(storage.readData(preparedRecord.fullPathTillResource())).thenReturn(inputStream);
        }

        service.download(preparedRecords, outputStream, "folder1/folder2/folder3/");

        for (ResourceDownloadService.PreparedFileRecord preparedRecord : preparedRecords) {
            verify(storage, times(1)).readData(preparedRecord.fullPathTillResource());
        }

        assertThat(outputStream.size()).isGreaterThan(0);
    }
}
