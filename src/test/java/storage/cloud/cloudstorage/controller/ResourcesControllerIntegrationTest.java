package storage.cloud.cloudstorage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.ResourceResponse;
import storage.cloud.cloudstorage.service.Type;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ResourcesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    private static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    @Container
    private static final MinIOContainer minio =
            new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.user", minio::getUserName);
        registry.add("minio.password", minio::getPassword);
        registry.add("minio.bucket.name", () -> "test-bucket");
    }

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Cookie sessionCookie;

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void start() throws Exception {
        String username = "tim1" + UUID.randomUUID();
        String passwordOriginal = "sadfasfkljkjl22##";
        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        this.sessionCookie = result.getResponse().getCookie("SESSION");
    }

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
    public void getFolderInfo() throws Exception {
        String parent = "";
        String folderName = "folder1";
        String type = Type.DIRECTORY.name();
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

        String parentFolder = folderName + "/";
        folderName = "child1";
        path = parentFolder + folderName + "/";

        mockMvc.perform(post("/directory")
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
        mockMvc.perform(post("/directory")
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
        MvcResult result = mockMvc.perform(get("/directory")
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

    @Test
    public void getFolderInfoFailsDueToNotAuthorizedUser() throws Exception {
        String path = "folder1/";

        mockMvc.perform(get("/directory")
                        .param("path", path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void getFolderInfoFailsSinceFolderDoesNotExists() throws Exception {
        String path = "folder1/";

        mockMvc.perform(get("/directory")
                        .cookie(sessionCookie)
                        .param("path", path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
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
