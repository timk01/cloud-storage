package storage.cloud.cloudstorage.controller.mvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import storage.cloud.cloudstorage.controller.DirectoryController;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.directory.DirectoryCreateService;
import storage.cloud.cloudstorage.service.directory.DirectoryGetInfoService;
import storage.cloud.cloudstorage.service.Type;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectoryController.class)
public class DirectoryCreateAndGettingInfoWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private DirectoryCreateService createService;

    @MockBean
    private DirectoryGetInfoService getInfoService;

    @Test
    public void creatingFolderOkWithSession() throws Exception {
        String parent = "";
        String folderName = "folder1";
        String type = Type.DIRECTORY.name();
        String path = folderName + "/";
        Long userId = 1L;

        ResourceResponse resourceResponse = ResourceResponse
                .builder()
                .path(parent)
                .name(folderName)
                .type(Type.DIRECTORY.name())
                .build();

        when(createService.createFolder(path, userId)).thenReturn(resourceResponse);

        mockMvc.perform(post("/api/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(parent))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(folderName))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(type));
    }

    @Test
    public void gettingFolderInfoOkWithSession() throws Exception {
        String path = "folder1/";
        Long userId = 1L;

        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();

        List<ResourceResponse> resourceResponses = List.of(
                ResourceResponse.builder()
                        .path(path)
                        .name("child1")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build());

        when(getInfoService.getFolderInfo(
                path,
                userId
        )).thenReturn(resourceResponses);

        mockMvc.perform(get("/api/directory")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value("child1"))
                .andExpect(jsonPath("$[0].type").value(Type.DIRECTORY.name()))
                .andExpect(jsonPath("$[1].path").value(path))
                .andExpect(jsonPath("$[1].name").value(gorgonFilename))
                .andExpect(jsonPath("$[1].size").value(1500L))
                .andExpect(jsonPath("$[1].type").value(gorgonType));
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void folderCreationFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(post("/api/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderInfoOkWithSessionAndEmptyPath() throws Exception {
        String path = "";
        Long userId = 1L;

        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();

        List<ResourceResponse> resourceResponses = List.of(
                ResourceResponse.builder()
                        .path(path)
                        .name("child1")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build());

        when(getInfoService.getFolderInfo(
                path,
                userId
        )).thenReturn(resourceResponses);

        mockMvc.perform(get("/api/directory")
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value("child1"))
                .andExpect(jsonPath("$[0].type").value(Type.DIRECTORY.name()))
                .andExpect(jsonPath("$[1].path").value(path))
                .andExpect(jsonPath("$[1].name").value(gorgonFilename))
                .andExpect(jsonPath("$[1].size").value(1500L))
                .andExpect(jsonPath("$[1].type").value(gorgonType));
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void gettingFolderFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(get("/api/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPath() {
        return Stream.of(
                //Arguments.of(""),
                Arguments.of("№№№/"),
                Arguments.of("abc")
        );
    }

    @Test
    public void folderCreationFailedDueToNoEmptyPath() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void folderCreationFailedDueToNoPath() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderFailedDueToNoPath() throws Exception {
        mockMvc.perform(get("/api/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void folderCreationFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/directory")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
