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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryGetInfoIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void getFolderInfo() throws Exception {
        String parent = "";
        String folderName = "folder1";
        String type = Type.DIRECTORY.name();
        String path = folderName + "/";

        mockMvc.perform(post("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andReturn();

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "gorgon.jpg";
        String gorgonType = Type.FILE.name();
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        mockMvc.perform(multipart("/api/resource")
                        .file(gorgon)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500))
                .andExpect(jsonPath("$[0].type").value(gorgonType));

        String parentFolder = folderName + "/";
        folderName = "child1";
        path = parentFolder + folderName + "/";

        mockMvc.perform(post("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(parentFolder))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(folderName))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(type));

        folderName = "child2";
        path = parentFolder + folderName + "/";
        mockMvc.perform(post("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(parentFolder))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(folderName))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(type));

        path = "folder1/";
        MvcResult result = mockMvc.perform(get("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn();

        String content = result.getResponse().getContentAsString();

        List<ResourceResponse> actualResponses
                = objectMapper.readValue(content, new TypeReference<List<ResourceResponse>>() {
        });

        List<ResourceResponse> expected = List.of(
                ResourceResponse.builder()
                        .path("folder1/")
                        .name(gorgonFilename)
                        .size(1500L)
                        .type(Type.FILE.name())
                        .build(),
                ResourceResponse.builder()
                        .path("folder1/")
                        .name("child1")
                        .type(Type.DIRECTORY.name())
                        .build(),
                ResourceResponse.builder()
                        .path("folder1/")
                        .name("child2")
                        .type(Type.DIRECTORY.name())
                        .build()
        );

        assertThat(actualResponses).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void getFolderInfoFailsDueToNotAuthorizedUser() throws Exception {
        String path = "folder1/";

        mockMvc.perform(get("/api/directory")
                        .param("path", path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void getFolderInfoFailsSinceFolderDoesNotExists() throws Exception {
        String path = "folder1/";

        mockMvc.perform(get("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
