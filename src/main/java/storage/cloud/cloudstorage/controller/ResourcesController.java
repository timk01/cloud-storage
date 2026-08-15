package storage.cloud.cloudstorage.controller;

import io.minio.errors.MinioException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.exception.InvalidFilesException;
import storage.cloud.cloudstorage.exception.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.ResourcesService;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping()
public class ResourcesController {
    private static final String PATH_VALIDATOR_REGEXP = "^([\\p{L}\\p{N}_\\s-]+/)+$";
    private static final String WRONG_PATH = "Wrong path is provided";


    private final ResourcesService service;


    @PostMapping("/directory")
    public ResponseEntity<ResourceResponse> createFolder(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotNull
            @Pattern(
                    regexp = PATH_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        ResourceResponse folder = service.createFolder(path, userId);

        return new ResponseEntity<>(
                folder,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/directory")
    public ResponseEntity<List<ResourceResponse>> getFolderInfo(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("path")
            @NotNull
            @Pattern(
                    regexp = PATH_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        List<ResourceResponse> folderInfo = service.getFolderInfo(path, userId);

        return new ResponseEntity<>(
                folderInfo,
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponse>> upload(
            @SessionAttribute(name = "userId", required = false) Long userId,
            @RequestParam("file") MultipartFile[] files,
            @RequestParam("path")
            @NotNull
            @Pattern(
                    regexp = PATH_VALIDATOR_REGEXP,
                    message = WRONG_PATH
            )
            String path
    ) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (userId == null) {
            throw new UnauthorizedActionException("User is not authorized");
        }

        if (files == null || files.length == 0 || files[0].isEmpty()) {
            throw new InvalidFilesException("Provided invalid files");
        }

        List<ResourceResponse> folderInfo = service.upload(path, files, userId);

        return new ResponseEntity<>(
                folderInfo,
                HttpStatus.OK
        );
    }
}
