package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import storage.cloud.cloudstorage.response.ResourceResponse;

import java.util.List;

@RequestMapping("/api")
public interface ResourcesApi {
    String PATH_UPLOAD_VALIDATOR_REGEXP = "^$|^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";
    String PATH_COMMON_VALIDATOR_REGEXP = "^[a-zA-Zа-яА-ЯёЁ0-9_\\s./-]+$";
    String WRONG_PATH = "Wrong path is provided";
    String INVALID_SYMBOLS_IN_PATH = "Invalid symbols in path are detected";

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
    ResponseEntity<List<ResourceResponse>> upload(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("object") MultipartFile[] files,
            @RequestParam("path")
            @Pattern(
                    regexp = PATH_UPLOAD_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    );

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
    ResponseEntity<List<ResourceResponse>> search(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("query")
            @NotBlank
            String query
    );

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
    ResponseEntity<ResourceResponse> move(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
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
    );

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
    ResponseEntity<StreamingResponseBody> download(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    );

    @Operation(summary = "Delete", description = "Delete resource(s) from path")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Resource(s) deleted"
            ),
            @ApiResponse(responseCode = "404", description = "Resource(s) not found")
    })
    @DeleteMapping(value = "/resource")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    );

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
    ResponseEntity<ResourceResponse> info(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_COMMON_VALIDATOR_REGEXP,
                    message = INVALID_SYMBOLS_IN_PATH
            )
            String path
    );
}
