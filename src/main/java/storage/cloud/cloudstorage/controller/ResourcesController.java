package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import static storage.cloud.cloudstorage.service.ResourceServiceUtils.extractName;

@Tag(name = "Resources")
@CommonApiErrorResponses
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api")
public class ResourcesController {

    private static final String PATH_UPLOAD_VALIDATOR_REGEXP = "^$|^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";
    private static final String PATH_COMMON_VALIDATOR_REGEXP = "^[a-zA-Zа-яА-ЯёЁ0-9_\\s./-]+$";
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
     * - all files are uploaded to the same directory specified by path;s (root upload = OK);
     * - existing file -> 409;
     * - multi-file upload is not atomic.
     */
    @Operation(summary = "Upload file(s)", description = "Upload file(s) to the specified resource path")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Files(s) uploaded",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponse.class)
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "File(s) already exists")
    })
    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponse>> upload(
            @Parameter(hidden = true)
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("object") MultipartFile[] files,
            @RequestParam("path")
            @Pattern(
                    regexp = PATH_UPLOAD_VALIDATOR_REGEXP,
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

    @Operation(summary = "Search", description = "Search resources by query")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search succeeded",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponse.class)
                            )
                    )
            )
    })
    @GetMapping(value = "/resource/search")
    public ResponseEntity<List<ResourceResponse>> search(
            @Parameter(hidden = true)
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

    @Operation(summary = "Rename/move", description = "Rename/move resource(s) from one path to another")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resource(s) renamed/moved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Resource(s) not found"),
            @ApiResponse(responseCode = "409", description = "Resource(s) already exists in destination path ")
    })
    @PostMapping(value = "/resource/move")
    public ResponseEntity<ResourceResponse> move(
            @Parameter(hidden = true)
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

    @Operation(summary = "Download", description = "Download resource(s) from path (folder in zip, solo files as is)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Downloaded resource(s)",
                    headers = @Header(
                            name = HttpHeaders.CONTENT_DISPOSITION,
                            description = "Attachment filename",
                            schema = @Schema(type = "string")
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Resource(s) not found")
    })
    @GetMapping(value = "/resource/download")
    public ResponseEntity<StreamingResponseBody> download(
            @Parameter(hidden = true)
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

    @Operation(summary = "Delete", description = "Delete resource(s) from path")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Resource(s) deleted"
            ),
            @ApiResponse(responseCode = "404", description = "Resource(s) not found")
    })
    @DeleteMapping(value = "/resource")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true)
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

    @Operation(summary = "Resource info", description = "Getting resource info")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resource(s) info is received",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Resource(s) not found")
    })
    @GetMapping(value = "/resource")
    public ResponseEntity<ResourceResponse> info(
            @Parameter(hidden = true)
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
