package storage.cloud.cloudstorage.controller.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import storage.cloud.cloudstorage.controller.ResourcesController;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;
import storage.cloud.cloudstorage.service.resource.*;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
class ResourceSearchingWebMvcTest {

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

        when(searchService.search(query, userId))
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
    public void searchingResourceFailedSinceUserIsUnauthorized() throws Exception {
        mockMvc.perform(get("/resource/search")
                        .param("query", "query"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}