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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.response.ResourceResponse;

import java.util.List;

@Tag(name = "Directories")
@CommonApiErrorResponses
@Validated
@RequestMapping("/api")
public interface DirectoryApi {

    String PATH_POST_STRICT_VALIDATOR_REGEXP = "^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";
    String PATH_GET_STRICT_VALIDATOR_REGEXP = "^$|^([a-zA-Zа-яА-ЯёЁ0-9_\\s.-]+/)+$";

    String WRONG_PATH = "Wrong path is provided";

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
    ResponseEntity<ResourceResponse> createFolder(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("path")
            @NotBlank
            @Pattern(
                    regexp = PATH_POST_STRICT_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    );

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
    ResponseEntity<List<ResourceResponse>> getFolderInfo(
            @Parameter(hidden = true)
            @CurrentUserId Long userId,
            @RequestParam("path")
            @Pattern(
                    regexp = PATH_GET_STRICT_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    );
}
