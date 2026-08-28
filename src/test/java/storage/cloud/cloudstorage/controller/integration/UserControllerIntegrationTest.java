package storage.cloud.cloudstorage.controller.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.Session;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.entity.User;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UsernameResponse;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder encoder;

    @Test
    public void registerUser() throws Exception {
        String username = "tim111";
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

        Long id = userRepository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void loginUserWithPrereg() throws Exception {
        String username = "tim111";
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

        Long id = userRepository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void loginUserWithAlreadyRegisteredUser() throws Exception {
        String username = "tim111";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        User user = userRepository.save(new User(username, encoded));

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

        Long id = userRepository.findByUsername(username).get().getId();
        Session redisSession = sessionRepository.findById(sessionId);

        assertThat(redisSession).isNotNull();
        Long sessionUserId = redisSession.getAttribute("userId");
        String sessionUsername = redisSession.getAttribute("username");
        assertThat(sessionUserId).isEqualTo(id);
        assertThat(sessionUsername).isEqualTo(username);
    }

    @Test
    public void registerUserFailsSinceUserExists() throws Exception {
        String username = "tim111";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        userRepository.save(new User(username, encoded));

        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void loginUserWithAlreadyRegisteredUserFailsSinceUseNameIsNotFound() throws Exception {
        String username = "tim111";
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
        String username = "tim111";
        String passwordOriginal = "sadfasfkljkjl22##";
        String encoded = encoder.encode(passwordOriginal);
        User user = userRepository.save(new User(username, encoded));

        String wrongPassword = "sadfasfkljkjl22##WRG";
        UserLoginRequest logDto = new UserLoginRequest(username, wrongPassword);
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}