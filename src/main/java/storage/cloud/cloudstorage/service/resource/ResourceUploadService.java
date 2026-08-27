package storage.cloud.cloudstorage.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.managed.InvalidFileNameException;
import storage.cloud.cloudstorage.repository.MinioRepository;
import storage.cloud.cloudstorage.repository.StorageInitializer;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.ArrayList;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.buildPreparedPath;
import static storage.cloud.cloudstorage.service.ResourceServiceUtils.buildPreparedRoot;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceUploadService {
    private final MinioRepository minioRepository;
    private final String minioBucketName;
    private final StorageInitializer initializer;

    public List<ResourceResponse> upload(String path, MultipartFile[] files, Long userId) {
        String preparedRoot = buildPreparedRoot(userId, minioBucketName);
        initializer.initStorage(preparedRoot);

        String preparedPath = buildPreparedPath(preparedRoot, path);
        List<PreparedFile> preparedFiles = getPreparedFiles(files, preparedPath);

        List<String> fullPathTillFile = preparedFiles.stream()
                .map(PreparedFile::fullPathTillFile)
                .toList();

        minioRepository.checkFiles(fullPathTillFile);

        List<MultipartFile> multipartFiles = preparedFiles.stream()
                .map(PreparedFile::multipartFile)
                .toList();
        minioRepository.upload(multipartFiles, fullPathTillFile);

        List<ResourceResponse> resources = new ArrayList<>();
        for (PreparedFile file : preparedFiles) {
            resources.add(
                    ResourceResponse
                            .builder()
                            .path(path)
                            .name(file.cleanedPathName())
                            .size(file.multipartFile().getSize())
                            .type(Type.FILE.name())
                            .build()
            );
        }

        log.info(
                "Resource upload is completed for user: userId={}, path={}; with filesQuantity={}",
                userId,
                path,
                preparedFiles.size()
        );


        return resources;
    }

    public record PreparedFile(
            String cleanedPathName,
            String fullPathTillFile,
            MultipartFile multipartFile
    ) {
    }

    @NotNull
    private List<PreparedFile> getPreparedFiles(MultipartFile[] files, String preparedPath) {
        List<PreparedFile> preparedFiles = new ArrayList<>();
        for (MultipartFile multipartFile : files) {

            String uncheckedFilename = multipartFile.getOriginalFilename();
            if (uncheckedFilename == null || uncheckedFilename.isEmpty()) {
                continue;
            }

            String cleanedPathName = StringUtils.cleanPath(uncheckedFilename);

            validateCleanedPathName(cleanedPathName);

            String fullPathTillFile = preparedPath + cleanedPathName;

            preparedFiles.add(
                    new PreparedFile(
                            cleanedPathName,
                            fullPathTillFile,
                            multipartFile
                    )
            );
        }
        return preparedFiles;
    }

    private void validateCleanedPathName(String cleanedPathName) {
        if (cleanedPathName.isEmpty() || ".".equals(cleanedPathName) || "..".equals(cleanedPathName)) {
            throw new InvalidFileNameException(
                    String.format(
                            "Multipartfile is a invalid: %s ", cleanedPathName
                    )
            );
        }
    }
}
