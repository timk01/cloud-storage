package storage.cloud.cloudstorage.controller.mvc;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import storage.cloud.cloudstorage.controller.ResourcesController;
import storage.cloud.cloudstorage.service.resource.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourcesController.class)
public class ResourceDownloadWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ResourceUploadService uploadService;

    @MockBean
    ResourceMoveService moveService;

    @MockBean
    ResourceSearchService searchService;

    @MockBean
    ResourceDownloadService downloadService;

    @MockBean
    ResourceDeleteService deleteService;

    @MockBean
    ResourceInfoService infoService;

    @Test
    public void downloadFileOkWithSession() throws Exception {
        String path = "folder1/folder2/folder3/file2.txt";
        Long userId = 1L;

        List<ResourceDownloadService.PreparedFileRecord> preparedFileRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "file2.txt",
                        "user-1-files/folder1/folder2/folder3/file2.txt"
                )
        );

        when(downloadService.prepareResource(
                path,
                userId
        )).thenReturn(preparedFileRecords);

        MvcResult result = mockMvc.perform(get("/api/resource/download")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_OCTET_STREAM.getMimeType()
                        )
                )
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"archive.zip\""
                        )
                );

        verify(downloadService).prepareResource(path, userId);
        verify(downloadService, times(1))
                .download(
                        eq(preparedFileRecords),
                        any(OutputStream.class)
                );
    }

    @Test
    public void downloadFolderOkWithSession() throws Exception {
        String path = "folder1/folder2/folder3/";
        Long userId = 1L;

        List<ResourceDownloadService.PreparedFileRecord> preparedFileRecords
                = List.of(
                new ResourceDownloadService.PreparedFileRecord(
                        "gorgon.jpg",
                        "user-1-files/folder1/folder2/folder3/gorgon.jpg"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/",
                        "user-1-files/folder1/folder2/folder3/newFolder/"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "newFolder/file2.txt",
                        "user-1-files/folder1/folder2/folder3/newFolder/file2.txt"
                ),
                new ResourceDownloadService.PreparedFileRecord(
                        "folder4/folder5/b.txt",
                        "user-1-files/folder1/folder2/folder3/folder4/folder5/b.txt"
                )
        );

        when(downloadService.prepareResource(
                path,
                userId
        )).thenReturn(preparedFileRecords);

        MvcResult result = mockMvc.perform(get("/api/resource/download")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(request().asyncStarted())
                .andReturn();


        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_OCTET_STREAM.getMimeType()
                        )
                )
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"archive.zip\""
                        )
                );

        verify(downloadService).prepareResource(path, userId);
        verify(downloadService, times(1))
                .download(
                        eq(preparedFileRecords),
                        any(OutputStream.class)
                );
    }

    @ParameterizedTest
    @MethodSource("invalidPathForDownloadResource")
    public void downloadResourceFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(get("/api/resource/download")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPathForDownloadResource() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("  "),
                Arguments.of("№№№/"),
                Arguments.of("abc@/")
        );
    }

    @Test
    public void downloadResourceFailedDueToNoPath() throws Exception {
        mockMvc.perform(get("/api/resource/download")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void downloadResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/resource/download")
                        .param("path", "folder1/folder2/folder3/file2.txt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
