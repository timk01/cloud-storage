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
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;
import storage.cloud.cloudstorage.service.resource.*;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
public class ResourceInfoWebMvcTest {

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
    public void gettingResourceInfoOkWithSessionForFile() throws Exception {
        String path = "folder9/folder10/test_file1.txt";
        Long userId = 1L;

        ResourceResponse resourceResponse =
                ResourceResponse.builder()
                        .path("folder9/folder10/")
                        .name("test_file1.txt")
                        .size(1000L)
                        .type(Type.FILE.name())
                        .build();

        when(infoService.resourceInfo(
                path,
                userId
        )).thenReturn(resourceResponse);

        mockMvc.perform(get("/resource")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("folder9/folder10/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("test_file1.txt"))
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.size").value(1000L))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.FILE.name()));
    }

    @Test
    public void gettingResourceInfoOkWithSessionForDirectory() throws Exception {
        String path = "folder9/folder10/folder3/";
        Long userId = 1L;

        ResourceResponse resourceResponse =
                ResourceResponse.builder()
                        .path("folder9/folder10/")
                        .name("folder3")
                        .type(Type.DIRECTORY.name())
                        .build();

        when(infoService.resourceInfo(
                path,
                userId
        )).thenReturn(resourceResponse);

        mockMvc.perform(get("/resource")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("folder9/folder10/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("folder3"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.DIRECTORY.name()));
    }

    @ParameterizedTest
    @MethodSource("invalidResourceGettingPath")
    public void gettingResourceFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(get("/resource")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidResourceGettingPath() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("  "),
                Arguments.of("№№№/"),
                Arguments.of("abc@/")
        );
    }

    @Test
    public void gettingResourceFailedDueToNoPath() throws Exception {
        mockMvc.perform(get("/resource")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/resource")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
