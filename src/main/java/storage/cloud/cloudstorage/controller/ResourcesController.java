package storage.cloud.cloudstorage.controller;

import io.minio.errors.MinioException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import storage.cloud.cloudstorage.exception.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.CreatedFolderResponse;
import storage.cloud.cloudstorage.response.CreatedResourceResponse;
import storage.cloud.cloudstorage.service.ResourcesService;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping()
public class ResourcesController {
    private static final String PATH_VALIDATOR_REGEXP = "^([\\p{L}\\p{N}_\\s-]+/)+$";
    private static final String WRONG_PATH = "Wrong path is provided";


    private final ResourcesService service;


    @PostMapping("/directory")
    public ResponseEntity<CreatedFolderResponse> createFolder(
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

        CreatedFolderResponse folder = service.createFolder(path, userId);

        return new ResponseEntity<>(
                folder,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/directory")
    public ResponseEntity<List<CreatedResourceResponse>> getFolderInfo(
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

        List<CreatedResourceResponse> folderInfo = service.getFolderInfo(path, userId);

        return new ResponseEntity<>(
                folderInfo,
                HttpStatus.OK
        );

    }
}

/*
GET /directory?path=$path

Ответ в случае успеха: 200 OK со следующим телом (application/json). Коллекция ресурсов, лежащих в папке (не рекурсивно):

[
  {
    "path": "folder1/folder2/", // путь к папке, в которой лежит ресурс
    "name": "file.txt",
    "size": 123, // размер файла в байтах. Если ресурс - папка, это поле отсутствует
    "type": "FILE" // DIRECTORY или FILE
  }
]

 */