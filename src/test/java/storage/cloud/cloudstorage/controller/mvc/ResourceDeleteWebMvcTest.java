package storage.cloud.cloudstorage.controller.mvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import storage.cloud.cloudstorage.controller.ResourcesController;
import storage.cloud.cloudstorage.service.resource.*;

import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
public class ResourceDeleteWebMvcTest {

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

    @Test
    public void deleteFileNoContentWithSession() throws Exception {
        String path = "folder1/folder2/folder3/file2.txt";
        Long userId = 1L;

        mockMvc.perform(delete("/resource")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isNoContent());

        verify(deleteService).delete(path, userId);
    }

    @Test
    public void deleteFolderNoContentWithSession() throws Exception {
        String path = "folder1/folder2/folder3/";
        Long userId = 1L;

        mockMvc.perform(delete("/resource")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isNoContent());

        verify(deleteService).delete(path, userId);
    }

    @ParameterizedTest
    @MethodSource("invalidPathForDeletedResource")
    public void deleteResourceFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(delete("/resource")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPathForDeletedResource() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("  "),
                Arguments.of("№№№/"),
                Arguments.of("abc@/")
        );
    }

    @Test
    public void deleteResourceFailedDueToNoPath() throws Exception {
        mockMvc.perform(delete("/resource")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void deleteResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/resource")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
