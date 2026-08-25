package storage.cloud.cloudstorage.controller.integration;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.service.Type;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ResourceDownloadIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void downloadingFileWentOk() throws Exception {
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
        MvcResult result = mockMvc.perform(get("/resource/download")
                        .cookie(sessionCookie)
                        .param("path", pathToFile))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult realResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_OCTET_STREAM.getMimeType()
                        )
                )
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"archive.zip\""
                        )
                )
                .andReturn();

        byte[] contentAsByteArray = realResult.getResponse().getContentAsByteArray();

        assertThat(contentAsByteArray).isNotEmpty();
        assertThat(contentAsByteArray[0]).isEqualTo((byte) 'P');
        assertThat(contentAsByteArray[1]).isEqualTo((byte) 'K');
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
        MvcResult result = mockMvc.perform(get("/resource/download")
                        .cookie(sessionCookie)
                        .param("path", pathToDirectory))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult realResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_OCTET_STREAM.getMimeType()
                        )
                )
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"archive.zip\""
                        )
                )
                .andReturn();

        byte[] contentAsByteArray = realResult.getResponse().getContentAsByteArray();

        List<String> names = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contentAsByteArray))) {
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                names.add(zipEntry.getName());
            }
        }

        assertThat(contentAsByteArray).isNotEmpty();
        assertThat(contentAsByteArray[0]).isEqualTo((byte) 'P');
        assertThat(contentAsByteArray[1]).isEqualTo((byte) 'K');

        assertThat(names).containsExactlyInAnyOrder(
                gorgonFilename,
                gorgonTxtFilename,
                "test_folder/"
        );
    }

    @Test
    public void downloadingEmptyDirectoryWentOk() throws Exception {
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

        MvcResult result = mockMvc.perform(get("/resource/download")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult realResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_OCTET_STREAM.getMimeType()
                        )
                )
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"archive.zip\""
                        )
                )
                .andReturn();

        byte[] contentAsByteArray = realResult.getResponse().getContentAsByteArray();


        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contentAsByteArray))) {
            assertThat(zis.getNextEntry()).isNull();
        }

        assertThat(contentAsByteArray).isNotEmpty();
        assertThat(contentAsByteArray[0]).isEqualTo((byte) 'P');
        assertThat(contentAsByteArray[1]).isEqualTo((byte) 'K');
    }
}
