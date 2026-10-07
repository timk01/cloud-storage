package storage.cloud.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.resource.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.extractName;

@RequiredArgsConstructor
@RestController
public class ResourcesController implements ResourcesApi {

    private final ResourceUploadService uploadService;
    private final ResourceSearchService searchService;
    private final ResourceMoveService moveService;
    private final ResourceDownloadService downloadService;
    private final ResourceDeleteService deleteService;
    private final ResourceInfoService infoService;

    /**
     * Upload contract:
     * - max file size: 5 MB, while max request size (all files): 30 MB;
     * - zero-byte files are allowed;
     * - all files are uploaded to the same directory specified by path;s (root upload = OK);
     * - existing file -> 409;
     * - multi-file upload is not atomic.
     */

    @Override
    public ResponseEntity<List<ResourceResponse>> upload(
            Long userId,
            MultipartFile[] files,
            String path
    ) {
        List<ResourceResponse> resourcesResponse = uploadService.upload(path, files, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourcesResponse);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> search(
            Long userId,
            String query
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        List<ResourceResponse> resourcesResponse = searchService.search(query, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourcesResponse);
    }

    @Override
    public ResponseEntity<ResourceResponse> move(
            Long userId,
            String fromPath,
            String toPath
    ) {
        ResourceResponse resourceResponse = moveService.move(fromPath, toPath, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> download(
            Long userId,
            String path
    ) {
        List<ResourceDownloadService.PreparedFileRecord> preparedResources
                = downloadService.prepareResource(path, userId);

        StreamingResponseBody responseBody = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                downloadService.download(preparedResources, outputStream, path);
            }
        };

        String filename = path.endsWith("/")
                ? "archive.zip"
                : extractName(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(responseBody);
    }

    @Override
    public ResponseEntity<Void> delete(
            Long userId,
            String path
    ) {
        deleteService.delete(path, userId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public ResponseEntity<ResourceResponse> info(
            Long userId,
            String path
    ) {
        ResourceResponse resourceResponse = infoService.resourceInfo(path, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }
}
