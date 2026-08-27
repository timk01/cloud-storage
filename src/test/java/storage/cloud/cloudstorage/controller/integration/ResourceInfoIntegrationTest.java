package storage.cloud.cloudstorage.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import storage.cloud.cloudstorage.service.Type;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceInfoIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void gettingResourceInfoForFileWentOk() throws Exception {
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

        mockMvc.perform(multipart("/resource")
                        .file(gorgonJpg)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))

                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500L))
                .andExpect(jsonPath("$[0].type").value(gorgonType));

        String pathToFile = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/"
                + gorgonFilename;

        mockMvc.perform(get("/resource")
                        .cookie(sessionCookie)
                        .param("path", pathToFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name").value(gorgonFilename))
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.size").value(1500L))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(gorgonType));
    }

    @Test
    public void gettingResourceInfoForFileDirectoryWentOk() throws Exception {
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

        mockMvc.perform(multipart("/resource")
                        .file(gorgonJpg)
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))

                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value(gorgonFilename))
                .andExpect(jsonPath("$[0].size").value(1500L))
                .andExpect(jsonPath("$[0].type").value(gorgonType));

        String pathToDirectory
                = "gorgon_root/gorgon_archive/gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000/";

        mockMvc.perform(get("/resource")
                        .cookie(sessionCookie)
                        .param("path", pathToDirectory))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.path").value("gorgon_root/gorgon_archive/"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.name")
                        .value("gorgon_files__timur_auto_550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.type").value(Type.DIRECTORY.name()));
    }
}
