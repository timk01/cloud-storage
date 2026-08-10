package storage.cloud.cloudstorage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.repository.UserRepository;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UsernameResponse;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder encoder;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    private static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void registerUser() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("SESSION");
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()));

        Long id = repository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void loginUserWithPrereg() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        UserRegisterRequest regDto = new UserRegisterRequest(username, passwordOriginal);

        MvcResult regResult = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        String content = regResult.getResponse().getContentAsString();

        UsernameResponse usernameResponse = objectMapper.readValue(content, UsernameResponse.class);
        String username1 = usernameResponse.username();

        UserLoginRequest logDto = new UserLoginRequest(username1, passwordOriginal);
        MvcResult logResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username1))
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie cookie = logResult.getResponse().getCookie("SESSION");
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()));

        Long id = repository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void loginUserWithAlreadyRegisteredUser() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        User user = repository.save(new User(username, encoded));

        UserLoginRequest logDto = new UserLoginRequest(username, passwordOriginal);
        MvcResult result = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("SESSION");
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()));

        Long id = repository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void registerUserFailsSinceUserExists() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        repository.save(new User(username, encoded));

        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void loginUserWithAlreadyRegisteredUserFailsSinceUseNameIsNotFound() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";

        UserLoginRequest logDto = new UserLoginRequest(username, passwordOriginal);
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void loginUserWithAlreadyRegisteredUserFailsSincePasswordIsWrong() throws Exception {
        String username = "tim1";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        User user = repository.save(new User(username, encoded));

        String wrongPassword = "sadfasfkljkjl22##WRONG_PASS";
        UserLoginRequest logDto = new UserLoginRequest(username, wrongPassword);
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}