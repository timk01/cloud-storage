package storage.cloud.cloudstorage.service.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;

import java.io.InputStream;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.buildPreparedRoot;

@RequiredArgsConstructor
@Service
public class ResourceDownloadService {

    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public InputStream download(String path, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        minioRepository.download();

        /*
        0. определиться файл или директория.
        1. получить файл (гетобжект ?)/рекурсивный список объектов (префикс - как уже делали)
        2.а. если файл вроде бы все проще и можно сразу писать обратно (с файла и начать!)
        2.б. урезать путь (относительно папки), тоже вроде то-то такое делали
        3. по идее по спискуу обжектов надо будет собирать конечный список (структуру) папок и хитро их упакоывать.
        как-пока не ясно
        3. разбираться с ZipStream и ZipEntry(relativePath)
         */

        return InputStream.nullInputStream();
    }
}
