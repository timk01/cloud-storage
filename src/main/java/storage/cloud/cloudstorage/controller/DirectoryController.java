package storage.cloud.cloudstorage.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryCreateService;
import storage.cloud.cloudstorage.service.directory.DirectoryGetInfoService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api")
public class DirectoryController {
    private static final String PATH_STRICT_VALIDATOR_REGEXP = "^([\\p{L}\\p{N}_\\s-]+/)+$";
    private static final String WRONG_PATH = "Wrong path is provided";

    private final DirectoryCreateService createFolderService;
    private final DirectoryGetInfoService getFolderInfoService;

    @PostMapping("/directory")
    public ResponseEntity<ResourceResponse> createFolder(
            @SessionAttribute(name = "userId", required = false) Long userId,
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

        ResourceResponse resourceResponse = createFolderService.createFolder(path, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceResponse);
    }

    @GetMapping("/directory")
    public ResponseEntity<List<ResourceResponse>> getFolderInfo(
            @SessionAttribute(name = "userId", required = false) Long userId,
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

        List<ResourceResponse> resourceResponse = getFolderInfoService.getFolderInfo(path, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceResponse);
    }
}
