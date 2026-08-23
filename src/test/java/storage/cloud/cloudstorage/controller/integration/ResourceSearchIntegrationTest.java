package storage.cloud.cloudstorage.controller.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceSearchIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void searchWideFoundAll() throws Exception {
        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";

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

        mockMvc.perform(multipart("/resource")
                        .file(gorgonJpg)
                        .file(gorgonTxt)
                        .cookie(sessionCookie)
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

        String query = "GoRgOn";
        MvcResult result = mockMvc.perform(get("/resource/search")
                        .cookie(sessionCookie)
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andReturn();

        String content = result.getResponse().getContentAsString();

        List<ResourceResponse> actualResponses
                = objectMapper.readValue(content, new TypeReference<>() {
        });

        List<ResourceResponse> expected = List.of(
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

        assertThat(actualResponses).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void searchWideFoundNothingSinceNothingMatchesQuery() throws Exception {
        String path = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";

        byte[] gorgonSize = new byte[1500];
        MockMultipartFile gorgonJpg = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        byte[] gorgonTxtSize = new byte[123];
        MockMultipartFile gorgonTxt = new MockMultipartFile(
                "file",
                "description_gorgon.txt",
                MediaType.TEXT_PLAIN_VALUE,
                gorgonTxtSize
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgonJpg)
                        .file(gorgonTxt)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));

        String query = "cat";
        mockMvc.perform(get("/resource/search")
                        .cookie(sessionCookie)
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

}
