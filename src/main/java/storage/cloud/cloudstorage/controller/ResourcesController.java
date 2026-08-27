package storage.cloud.cloudstorage.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.resource.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api")
public class ResourcesController {
    private static final String PATH_STRICT_VALIDATOR_REGEXP = "^([\\p{L}\\p{N}_\\s-]+/)+$";
    private static final String PATH_COMMON_VALIDATOR_REGEXP = "^[\\p{L}\\p{N}_\\s./-]+$";
    private static final String WRONG_PATH = "Wrong path is provided";
    private static final String INVALID_SYMBOLS_IN_PATH = "Invalid symbols in path are detected";

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
     * - all files are uploaded to the same directory specified by path;s;
     * - existing file -> 409;
     * - multi-file upload is not atomic.
     */
    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponse>> upload(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("file") MultipartFile[] files,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_STRICT_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        List<ResourceResponse> resourcesResponse = uploadService.upload(path, files, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourcesResponse);
    }

    @GetMapping(value = "/resource/search")
    public ResponseEntity<List<ResourceResponse>> search(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("query")
            @NotBlank
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

    @PostMapping(value = "/resource/move")
    public ResponseEntity<ResourceResponse> move(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("from")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String fromPath,
            @RequestParam("to")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String toPath
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        ResourceResponse resourceResponse = moveService.move(fromPath, toPath, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }

    @GetMapping(value = "/resource/download")
    public ResponseEntity<StreamingResponseBody> download(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        List<ResourceDownloadService.PreparedFileRecord> preparedResources
                = downloadService.prepareResource(path, userId);

        StreamingResponseBody responseBody = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                downloadService.download(preparedResources, outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archive.zip\"")
                .body(responseBody);
    }

    @DeleteMapping(value = "/resource")
    public ResponseEntity<Void> delete(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        deleteService.delete(path, userId);

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping(value = "/resource")
    public ResponseEntity<ResourceResponse> info(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        ResourceResponse resourceResponse = infoService.resourceInfo(path, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }
}
