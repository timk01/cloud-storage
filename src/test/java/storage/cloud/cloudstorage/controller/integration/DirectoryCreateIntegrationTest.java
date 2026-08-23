package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import storage.cloud.cloudstorage.service.Type;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryCreateIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void createFolder() throws Exception {
        String parent = "";
        String folderName = "folder1";
        String type = Type.DIRECTORY.name();
        String path = folderName + "/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
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
    public void createFolderFailsDueToNotAuthorizedUser() throws Exception {
        String path = "folder1/";

        mockMvc.perform(post("/directory")
                        .param("path", path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void createFolderFailsDueToNonExistedParenFolder() throws Exception {
        String path = "folder1/folder2/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void createFolderFailsSinceFolderAlreadyExists() throws Exception {
        String path = "folder1/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}
