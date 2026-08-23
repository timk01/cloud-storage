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
import storage.cloud.cloudstorage.service.resource.ResourceMoveService;
import storage.cloud.cloudstorage.service.resource.ResourceSearchService;
import storage.cloud.cloudstorage.service.resource.ResourceUploadService;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
public class ResourceMoveWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ResourceUploadService uploadService;

    @MockBean
    ResourceSearchService searchService;

    @MockBean
    ResourceMoveService moveService;

    @Test
    public void movingFileOkWithSession() throws Exception {
        String pathFrom = "folder1/folder2/folder3/file1.txt";
        String pathTo = "folder9/folder10/test_file1.txt";
        Long userId = 1L;


        ResourceResponse resourceResponse =
                ResourceResponse.builder()
                        .path("folder9/folder10/")
                        .name("test_file1.txt")
                        .size(1000L)
                        .type(Type.FILE.name())
                        .build();

        when(moveService.move(
                pathFrom,
                pathTo,
                userId
        )).thenReturn(resourceResponse);

        mockMvc.perform(post("/resource/move")
                        .sessionAttr("userId", userId)
                        .param("from", pathFrom)
                        .param("to", pathTo))
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
    public void movingFolderOkWithSession() throws Exception {
        String pathFrom = "folder1/folder2/folder3/";
        String pathTo = "folder9/folder10/folder3/";
        Long userId = 1L;


        ResourceResponse resourceResponse =
                ResourceResponse.builder()
                        .path("folder9/folder10/")
                        .name("folder3")
                        .type(Type.DIRECTORY.name())
                        .build();

        when(moveService.move(
                pathFrom,
                pathTo,
                userId
        )).thenReturn(resourceResponse);

        mockMvc.perform(post("/resource/move")
                        .sessionAttr("userId", userId)
                        .param("from", pathFrom)
                        .param("to", pathTo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("folder9/folder10/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("folder3"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.DIRECTORY.name()));
    }

    @ParameterizedTest
    @MethodSource("invalidPathForMovingResource")
    public void movingResourceFailedDueToInvalidPath(String pathFrom, String pathTo) throws Exception {
        mockMvc.perform(post("/resource/move")
                        .sessionAttr("userId", 1L)
                        .param("from", pathFrom)
                        .param("to", pathTo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPathForMovingResource() {
        return Stream.of(
                Arguments.of("folder1/", ""),
                Arguments.of("", "folder1/"),

                Arguments.of("folder1/", "  "),
                Arguments.of("  ", "folder1/"),

                Arguments.of("folder1/", "№№№/"),
                Arguments.of("№№№/", "folder1/"),

                Arguments.of("folder1/", "abc@/"),
                Arguments.of("abc@/", "folder1/")
        );
    }

    @Test
    public void movingResourceFailedDueToNoFromPath() throws Exception {
        mockMvc.perform(post("/resource/move")
                        .sessionAttr("userId", 1L)
                        .param("to", "abc/"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void movingResourceFailedDueToNoToPath() throws Exception {
        mockMvc.perform(post("/resource/move")
                        .sessionAttr("userId", 1L)
                        .param("from", "abc/"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void movingResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/resource/move")
                        .param("from", "abc/")
                        .param("to", "abc2/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
