package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import storage.cloud.cloudstorage.service.Type;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceMoveIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void fileMovingToNewPathWentOk() throws Exception {
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

        String pathFrom = path + "description_gorgon.txt";
        String pathTo = "folder9/folder10/new_gorgon_description.txt";

        mockMvc.perform(post("/resource/move")
                        .cookie(sessionCookie)
                        .param("from", pathFrom)
                        .param("to", pathTo))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("folder9/folder10/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("new_gorgon_description.txt"))
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.size").value(123L))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.FILE.name()));

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500L))
                .andExpect(jsonPath("$[0].type").value(Type.FILE.name()));

        String newDirectoryPath = "folder9/folder10/";

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", newDirectoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value(newDirectoryPath))
                .andExpect(jsonPath("$[0].name").value("new_gorgon_description.txt"))
                .andExpect(jsonPath("$[0].size").value(123L))
                .andExpect(jsonPath("$[0].type").value(Type.FILE.name()));
    }

    @Test
    public void directoryMovingToNewPathWentOk() throws Exception {
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

        String pathFrom = path;
        String pathTo = "folder9/folder10/";

        mockMvc.perform(post("/resource/move")
                        .cookie(sessionCookie)
                        .param("from", pathFrom)
                        .param("to", pathTo))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("folder9/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value("folder10"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.DIRECTORY.name()));

        String newDirectoryPath = "folder9/folder10/";

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", newDirectoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].path").value(newDirectoryPath))
                .andExpect(jsonPath("$[0].name").value(gorgonTxtFilename))
                .andExpect(jsonPath("$[0].size").value(123L))
                .andExpect(jsonPath("$[0].type").value(Type.FILE.name()))

                .andExpect(jsonPath("$[1].path").value(newDirectoryPath))
                .andExpect(jsonPath("$[1].name").value(gorgonFilename))
                .andExpect(jsonPath("$[1].size").value(1500L))
                .andExpect(jsonPath("$[1].type").value(Type.FILE.name()))

                .andExpect(jsonPath("$[2].path").value(newDirectoryPath))
                .andExpect(jsonPath("$[2].name").value("test_folder"))
                .andExpect(jsonPath("$[2].type").value(Type.DIRECTORY.name()));

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", "folder9/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("folder9/"))
                .andExpect(jsonPath("$[0].name").value("folder10"))
                .andExpect(jsonPath("$[0].type").value(Type.DIRECTORY.name()));

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNotFound());
    }
}
