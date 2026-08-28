package storage.cloud.cloudstorage.controller.mvc;

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
import storage.cloud.cloudstorage.controller.ResourcesController;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;
import storage.cloud.cloudstorage.service.resource.*;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
public class ResourceUploadingWebMvcTest {

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
    public void uploadingResourceThinOkWithSession() throws Exception {
        String path = "folder1/";
        Long userId = 1L;

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();
        MockMultipartFile gorgon = new MockMultipartFile(
                "object",
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

        when(uploadService.upload(
                eq(path),
                any(MultipartFile[].class),
                eq(userId))
        ).thenReturn(resourceResponses);


        mockMvc.perform(multipart("/api/resource")
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
                "object",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        byte[] gorgonTxtSize = new byte[123];
        String gorgonTxtFilename = "description_gorgon.txt";
        String gorgonTxtType = Type.FILE.name();
        MockMultipartFile gorgonTxt = new MockMultipartFile(
                "object",
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

        when(uploadService.upload(
                eq(path),
                any(MultipartFile[].class),
                eq(userId))
        ).thenReturn(resourceResponses);

        mockMvc.perform(multipart("/api/resource")
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
    public void uploadingResourceOkWithSessionAndEmptyPath() throws Exception {
        String path = "";
        Long userId = 1L;

        MockMultipartFile gorgon = new MockMultipartFile(
                "object",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        List<ResourceResponse> resourceResponses = List.of(
                ResourceResponse.builder()
                        .path(path)
                        .name("gorgon.jpg")
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build()
        );

        when(uploadService.upload(
                eq(path),
                any(MultipartFile[].class),
                eq(userId))
        ).thenReturn(resourceResponses);

        mockMvc.perform(multipart("/api/resource")
                        .file(gorgon)
                        .sessionAttr("userId", userId)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(""));

    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void uploadingResourceFailedDueToInvalidPath(String path) throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "object",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/api/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPath() {
        return Stream.of(
                Arguments.of("№№№/"),
                Arguments.of("abc")
        );
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

        mockMvc.perform(multipart("/api/resource")
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
    public void uploadingResourceFailedDueToInvalidPath() throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "object",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/api/resource")
                        .file(gorgon)
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void uploadingResourceFailedSinceUserIsUnauthorized() throws Exception {
        MockMultipartFile gorgon = new MockMultipartFile(
                "object",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[1500]
        );

        mockMvc.perform(multipart("/api/resource")
                        .file(gorgon)
                        .param("path", "folder1/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
