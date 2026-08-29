package storage.cloud.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ErrorResponse;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryCreateService;
import storage.cloud.cloudstorage.service.directory.DirectoryGetInfoService;

import java.util.List;

@Tag(name = "Directories")
@CommonApiErrorResponses
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api")
public class DirectoryController {
    private static final String PATH_POST_STRICT_VALIDATOR_REGEXP = "^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";
    private static final String PATH_GET_STRICT_VALIDATOR_REGEXP = "^$|^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";

    private static final String WRONG_PATH = "Wrong path is provided";

    private final DirectoryCreateService createFolderService;
    private final DirectoryGetInfoService getFolderInfoService;

    @Operation(summary = "Create directory", description = "Creates a directory at the specified path")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Directory created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Parent folder does not exist"),
            @ApiResponse(responseCode = "409", description = "Folder already exists")
    })
    @PostMapping("/directory")
    public ResponseEntity<ResourceResponse> createFolder(
            @Parameter(hidden = true)
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_POST_STRICT_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        ResourceResponse resourceResponse = createFolderService.createFolder(path, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceResponse);
    }

    @Operation(
            summary = "Get directory",
            description = "Gets a directory info at the specified path"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Directory info is received",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ResourceResponse.class)
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Folder does not exist"),
    })
    @GetMapping("/directory")
    public ResponseEntity<List<ResourceResponse>> getFolderInfo(
            @Parameter(hidden = true)
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @Pattern(
                    regexp = PATH_GET_STRICT_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        List<ResourceResponse> resourceResponse = getFolderInfoService.getFolderInfo(path, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }
}
