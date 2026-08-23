package storage.cloud.cloudstorage.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.request.UserRegisterRequest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    protected static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    protected static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    protected static final MinIOContainer minio =
            new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");

    static {
        postgres.start();
        redis.start();
        minio.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );

        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.user", minio::getUserName);
        registry.add("minio.password", minio::getPassword);
        registry.add("minio.bucket.name", () -> "test-bucket");
    }

    @Autowired
    protected SessionRepository<? extends Session> sessionRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Cookie sessionCookie;

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
}
