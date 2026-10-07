package storage.cloud.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import storage.cloud.cloudstorage.exception.managed.UnauthorizedActionException;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryCreateService;
import storage.cloud.cloudstorage.service.directory.DirectoryGetInfoService;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class DirectoryController implements DirectoryApi {

    private final DirectoryCreateService createFolderService;
    private final DirectoryGetInfoService getFolderInfoService;

    @Override
    public ResponseEntity<ResourceResponse> createFolder(
            Long userId,
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

    @Override
    public ResponseEntity<List<ResourceResponse>> getFolderInfo(
            Long userId,
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
