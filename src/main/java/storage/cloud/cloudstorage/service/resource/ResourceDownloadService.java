package storage.cloud.cloudstorage.service.resource;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.exception.technical.ResourceDownloadException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.service.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceDownloadService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public void download(List<PreparedFileRecord> preparedFileRecords, OutputStream outputStream, String path) {
        try {
            if (preparedFileRecords.isEmpty()) {
                String folder = extractName(removeTrailingSlash(path)) + "/";

                processEmptyFolder(folder, outputStream);
            } else if ("FILE".equals(preparedFileRecords.getFirst().type())) {
                processFile(preparedFileRecords, outputStream);
            } else {
                processFolder(preparedFileRecords, outputStream);
            }
        } catch (IOException ioException) {
            throw new ResourceDownloadException("Failed to download resource", ioException);
        }

        log.info(
                "Download stream is completed"
        );
    }

    private void processEmptyFolder(String archiveName, OutputStream outputStream)
            throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        zos.putNextEntry(new ZipEntry(archiveName));

        zos.closeEntry();

        zos.finish();
    }

    private void processFile(List<PreparedFileRecord> preparedFileRecords, OutputStream outputStream)
            throws IOException {
        PreparedFileRecord file = preparedFileRecords.getFirst();
        try (InputStream inputStream = minioRepository.readData(file.fullPathTillResource())) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void processFolder(List<PreparedFileRecord> preparedFileRecords, OutputStream outputStream)
            throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        for (PreparedFileRecord fileRecord : preparedFileRecords) {
            try (InputStream inputStream = minioRepository.readData(fileRecord.fullPathTillResource())) {

                ZipEntry entry = new ZipEntry(fileRecord.pathForArchive());
                zos.putNextEntry(entry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
            }
        }

        zos.finish();
    }

    public List<PreparedFileRecord> prepareResource(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String fullPathTo = buildPreparedPath(preparedRoot, path);

        String type = path.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

        validateResourceExists(
                minioRepository.doesPathExist(fullPathTo), fullPathTo
        );

        log.info(
                "Resource is validated for download: userId={}, path={}; with type={}",
                userId,
                path,
                type
        );

        if ("FILE".equals(type)) {
            return Collections.singletonList(
                    new PreparedFileRecord(
                            extractName(path),
                            fullPathTo,
                            type
                    )
            );
        }

        List<Item> items = minioRepository.search(fullPathTo);

        List<PreparedFileRecord> paths = new ArrayList<>();
        for (Item item : items) {
            String fullObjectName = item.objectName();
            String relativePath = fullObjectName.substring(fullPathTo.length());

            if (!relativePath.isEmpty()) {
                PreparedFileRecord record = new PreparedFileRecord(relativePath, fullObjectName, type);
                paths.add(record);
            }
        }

        return paths;
    }

    public record PreparedFileRecord(
            String pathForArchive,

            String fullPathTillResource,

            String type
    ) {
    }
}
