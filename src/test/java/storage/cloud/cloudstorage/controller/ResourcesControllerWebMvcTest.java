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
    public void uploadingResourceThinOkWithSession() throws Exception {
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
    public void uploadingResourceWideOkWithSession() throws Exception {
        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();
        MockMultipartFile gorgonJpg = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        byte[] gorgonTxtSize = new byte[123];
        String gorgonTxtFilename = "description_gorgon.txt";
        String gorgonTxtType = Type.FILE.name();
        MockMultipartFile gorgonTxt = new MockMultipartFile(
                "file",
                "description_gorgon.txt",
                MediaType.TEXT_PLAIN_VALUE,
                gorgonTxtSize
        );

        List<ResourceResponse> resourceResponses = List.of(
                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(gorgonType)
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonTxtFilename)
                        .size(123L)
                        .type(gorgonTxtType)
                        .build()
        );

        when(resourcesService.upload(
                eq(path),
                any(MultipartFile[].class),
                eq(userId))
        ).thenReturn(resourceResponses);

        mockMvc.perform(multipart("/resource")
                        .file(gorgonJpg)
                        .file(gorgonTxt)
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))

                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500L))
                .andExpect(jsonPath("$[0].type").value(gorgonType))

                .andExpect(jsonPath("$[1].path").value(path))
                .andExpect(jsonPath("$[1].name").value(gorgonTxtFilename))
                .andExpect(jsonPath("$[1].size").value(123L))
                .andExpect(jsonPath("$[1].type").value(gorgonTxtType));
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

    @Test
    public void searchingResourceByMaskOkWithSession() throws Exception {
        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        String query = "gorgon";
        Long userId = 1L;

        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();

        String gorgonTxtFilename = "description_gorgon.txt";
        String gorgonTxtType = Type.FILE.name();

        List<ResourceResponse> searchResponses = List.of(
                ResourceResponse.builder()
                        .path("")
                        .name("gorgon_root")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path("gorgon_root/")
                        .name("gorgon_archive")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path("gorgon_root/gorgon_archive/")
                        .name("gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000")
                        .type(Type.DIRECTORY.name())
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(gorgonType)
                        .build(),

                ResourceResponse.builder()
                        .path(path)
                        .name(gorgonTxtFilename)
                        .size(123L)
                        .type(gorgonTxtType)
                        .build()
        );

        when(resourcesService.search(query, userId))
                .thenReturn(searchResponses);

        mockMvc.perform(get("/resource/search")
                        .sessionAttr("userId", userId)
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].path").value(""))
                .andExpect(jsonPath("$[0].name").value("gorgon_root"))
                .andExpect(jsonPath("$[0].type").value(Type.DIRECTORY.name()))
                .andExpect(jsonPath("$[0].size").doesNotExist())

                .andExpect(jsonPath("$[1].path").value("gorgon_root/"))
                .andExpect(jsonPath("$[1].name").value("gorgon_archive"))
                .andExpect(jsonPath("$[1].type").value(Type.DIRECTORY.name()))
                .andExpect(jsonPath("$[1].size").doesNotExist())

                .andExpect(jsonPath("$[2].path").value("gorgon_root/gorgon_archive/"))
                .andExpect(jsonPath("$[2].name")
                        .value("gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$[2].type").value(Type.DIRECTORY.name()))
                .andExpect(jsonPath("$[2].size").doesNotExist())

                .andExpect(jsonPath("$[3].path").value(path))
                .andExpect(jsonPath("$[3].name").value(gorgonFilename))
                .andExpect(jsonPath("$[3].size").value(1500L))
                .andExpect(jsonPath("$[3].type").value(gorgonType))

                .andExpect(jsonPath("$[4].path").value(path))
                .andExpect(jsonPath("$[4].name").value(gorgonTxtFilename))
                .andExpect(jsonPath("$[4].size").value(123L))
                .andExpect(jsonPath("$[4].type").value(gorgonTxtType));
    }

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

        when(resourcesService.move(
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

        when(resourcesService.move(
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
    public void searchingResourceFailedDueToEmptyQuery() throws Exception {
        mockMvc.perform(get("/resource/search")
                        .sessionAttr("userId", 1L)
                        .param("query", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void searchingResourceFailedDueToNoQuery() throws Exception {
        mockMvc.perform(get("/resource/search")
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

    @Test
    public void searchingResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/resource/search")
                        .param("query", "query"))
                .andExpect(status().isUnauthorized())
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