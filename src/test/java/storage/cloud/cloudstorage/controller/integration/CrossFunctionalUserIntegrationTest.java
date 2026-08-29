package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.service.Type;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CrossFunctionalUserIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void anotherUserCanNotGetResourceInfoFromFirstUser() throws Exception {
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
                "object",
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
        mockMvc.perform(get("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        String username = "an_usr" + System.currentTimeMillis();
        String passwordOriginal = "sadfasfkljkjl22##";
        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        sessionCookie = result.getResponse().getCookie("SESSION");

        mockMvc.perform(get("/api/resource/search")
                        .cookie(sessionCookie)
                        .param("query", gorgonFilename))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/directory")
                        .cookie(sessionCookie)
                        .param("path", "folder1/"))
                .andExpect(status().isNotFound());
    }
}
