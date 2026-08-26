package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import storage.cloud.cloudstorage.service.Type;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceDeleteIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void deletingFileWentOk() throws Exception {
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

        String newFolderName = "test_folder";
        String type = Type.DIRECTORY.name();
        String requestPath = path + newFolderName + "/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", requestPath))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(newFolderName))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(type));

        String pathToFile = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/"
                + gorgonFilename;

        mockMvc.perform(delete("/resource")
                        .cookie(sessionCookie)
                        .param("path", pathToFile))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))

                .andExpect(jsonPath("$[?(@.type == 'FILE')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.type == 'FILE')].name")
                        .value(hasItem(gorgonTxtFilename)))

                .andExpect(jsonPath("$[?(@.type == 'DIRECTORY')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.type == 'DIRECTORY')].name")
                        .value(hasItem(newFolderName)));
    }

    @Test
    public void downloadingDirectoryWentOk() throws Exception {
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

        String newFolderName = "test_folder";
        String type = Type.DIRECTORY.name();
        String requestPath = path + newFolderName + "/";

        mockMvc.perform(post("/directory")
                        .cookie(sessionCookie)
                        .param("path", requestPath))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(newFolderName))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(type));

        String pathToDirectory = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";
        mockMvc.perform(delete("/resource")
                        .cookie(sessionCookie)
                        .param("path", pathToDirectory))
                .andExpect(status().isNoContent());

        String pathToDirectoryParent = "gorgon_root/gorgon_archive/";
        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", pathToDirectoryParent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void deletingEmptyDirectoryWentOk() throws Exception {
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

        mockMvc.perform(delete("/resource")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
