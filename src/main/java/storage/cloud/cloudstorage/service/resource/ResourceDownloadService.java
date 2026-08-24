package storage.cloud.cloudstorage.service.resource;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.exception.SourceResourceNotFoundException;
import storage.cloud.cloudstorage.exception.StorageException;
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

@RequiredArgsConstructor
@Service
public class ResourceDownloadService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public void download(List<PreparedFileRecord> preparedFileRecords, OutputStream outputStream) {
        ZipOutputStream zos = new ZipOutputStream(outputStream);

        try {
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
        } catch (IOException ioException) {
            throw new StorageException("Failed to download resource", ioException);
        }
    }

        public List<PreparedFileRecord> prepareResource (String path, Long userId){
            String preparedRoot = buildPreparedRoot(userId, minioBucketName);
            initializer.initStorage(preparedRoot);

            String fullPathTo = buildPreparedPath(preparedRoot, path);

            String type = path.endsWith("/") ? Type.DIRECTORY.name() : Type.FILE.name();

            boolean doesPathExist = minioRepository.doesPathExist(fullPathTo);
            checkResource(doesPathExist, fullPathTo);
            if ("FILE".equals(type)) {
                return Collections.singletonList(
                        new PreparedFileRecord(
                                extractName(path),
                                fullPathTo)
                );
            }

            List<Item> items = minioRepository.search(fullPathTo);

            List<PreparedFileRecord> paths = new ArrayList<>();
            for (Item item : items) {
                String fullObjectName = item.objectName();
                String relativePath = fullObjectName.substring(fullPathTo.length());

                if (!relativePath.isEmpty()) {
                    PreparedFileRecord record = new PreparedFileRecord(relativePath, fullObjectName);
                    paths.add(record);
                }
            }

            return paths;
        }

        private void checkResource ( boolean doesPathExist, String path){
            if (!doesPathExist) {
                throw new SourceResourceNotFoundException(
                        String.format(
                                "Resource is not found by path: %s ", path
                        )
                );
            }
        }

        public record PreparedFileRecord(
                String pathForArchive,

                String fullPathTillResource
        ) {
        }
    }
