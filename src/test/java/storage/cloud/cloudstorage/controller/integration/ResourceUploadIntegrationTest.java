package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import storage.cloud.cloudstorage.service.Type;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceUploadIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void uploadFile() throws Exception {
        String folderName = "folder1";
        String path = folderName + "/";

        mockMvc.perform(post("/directory")
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

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500))
                .andExpect(jsonPath("$[0].type").value(gorgonType));
    }

    @Test
    public void uploadFileRecursive() throws Exception {
        String folderName = "folder1";
        String path = folderName + "/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andReturn();

        byte[] gorgonSize = new byte[1500];
        String gorgonFilename = "upload_folder/gorgon.jpg";
        String gorgonType = Type.FILE.name();
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "upload_folder/gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500))
                .andExpect(jsonPath("$[0].type").value(gorgonType));
    }

    @Test
    public void uploadFailedSinceFailedWasAlreadyUploaded() throws Exception {
        String folderName = "folder1";
        String path = folderName + "/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andReturn();

        byte[] gorgonSize = new byte[1500];
        MockMultipartFile gorgon = new MockMultipartFile(
                "file",
                "gorgon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                gorgonSize
        );

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/resource")
                        .file(gorgon)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}
