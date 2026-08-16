package storage.cloud.cloudstorage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.ResourcesService;
import storage.cloud.cloudstorage.service.Type;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
class ResourcesControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private ResourcesService resourcesService;

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

        when(resourcesService.createFolder(path, userId)).thenReturn(resourceResponse);

        mockMvc.perform(post("/directory")
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
    public void uploadingFolderOkWithSession() throws Exception {
        String path = "folder1/";
        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        List<ResourceResponse> resourceResponses = List.of(ResourceResponse.builder()
                .path("folder1/")
                .name(gorgonFilename)
                .size(1500L)
                .type(Type.FILE.name())
                .build());

        when(resourcesService.upload(
                eq(path),
                any(MultipartFile[].class),
                eq(userId))
        ).thenReturn(resourceResponses);


        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").exists())
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").exists())
                .andExpect(jsonPath("$[0].size").value(1500L))
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].type").value(gorgonType));
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

        when(resourcesService.getFolderInfo(
                path,
                userId
        )).thenReturn(resourceResponses);


        mockMvc.perform(get("/directory")
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

    private static Stream<Arguments> invalidPath() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("№№№/"),
                Arguments.of("abc")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void folderCreationFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(post("/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void gettingFolderFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(get("/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void uploadingResourceFailedDueToInvalidPath(String path) throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void uploadingResourceFailedDueToWrongMultipartFileName() throws Exception {
        String path = "folder1/";

        MockMultipartFile gorgon = new MockMultipartFile(
                "wrong_file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * No paths at all!
     *
     * @throws Exception
     */

    @Test
    public void folderCreationFailedDueToNoPath() throws Exception {
        mockMvc.perform(post("/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderFailedDueToInvalidPath() throws Exception {
        mockMvc.perform(get("/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void uploadingResourceFailedDueToInvalidPath() throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void folderCreationFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/directory")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/directory")
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void uploadingResourceFailedSinceUserIsUnauthorized() throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}