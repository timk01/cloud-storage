package storage.cloud.cloudstorage.controller.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.controller.UserController;
import storage.cloud.cloudstorage.request.UserLoginRequest;
import storage.cloud.cloudstorage.request.UserRegisterRequest;
import storage.cloud.cloudstorage.response.UserResponse;
import storage.cloud.cloudstorage.service.UserService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerWebMvcTest {

    private static final String VALID_USERNAME = "tim11";
    private static final String VALID_PASSWORD = "sadfasfkljkjl22##";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void registrationIsSucceeded() throws Exception {
        String username = "tim11";
        String passwordOriginal = "sadfasfkljkjl22##";
        UserRegisterRequest dto = new UserRegisterRequest(username, passwordOriginal);

        UserResponse userResponse = new UserResponse(1L, username);

        when(userService.register(dto)).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    public void loginIsSucceeded() throws Exception {
        String username = "tim11";
        String passwordOriginal = "sadfasfkljkjl22##";
        UserLoginRequest dto = new UserLoginRequest(username, passwordOriginal);

        UserResponse userResponse = new UserResponse(1L, username);

        when(userService.login(dto)).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    public void gettingUserIsOkIfItHasSession() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .sessionAttr("userId", 1L)
                        .sessionAttr("username", "tim1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.username").value("tim1"));
    }

    @Test
    public void gettingUserIsFailedIfItHasNoSession() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void gettingUserIsFailedIfItHasPartSession1() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void gettingUserFailedIfItHasPartSession2() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .sessionAttr("username", "tim1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loggingUserIsOkIfItHasSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/sign-out")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isNoContent())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);

        assertThat(session).isNull();
    }

    @Test
    public void loggingUserFailedIfItHasNoSession() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationData")
    public void registrationFailedDueToInvalidData(String username, String password) throws Exception {
        UserRegisterRequest dto = new UserRegisterRequest(username, password);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidRegistrationData() {
        return Stream.of(
                Arguments.of("", VALID_PASSWORD),
                Arguments.of("aaaa", VALID_PASSWORD),
                Arguments.of("a".repeat(21), VALID_PASSWORD),
                Arguments.of("tim-1", VALID_PASSWORD),

                Arguments.of(VALID_USERNAME, ""),
                Arguments.of(VALID_USERNAME, "aaaa"),
                Arguments.of(VALID_USERNAME, "a".repeat(21)),
                Arguments.of(VALID_USERNAME, "abc 12")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidLoginData")
    public void loginFailedDueToInvalidData(String username, String password) throws Exception {
        UserLoginRequest dto = new UserLoginRequest(username, password);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidLoginData() {
        return Stream.of(
                Arguments.of("", VALID_PASSWORD),
                Arguments.of("aaaa", VALID_PASSWORD),
                Arguments.of("a".repeat(21), VALID_PASSWORD),
                Arguments.of("tim-1", VALID_PASSWORD),

                Arguments.of(VALID_USERNAME, ""),
                Arguments.of(VALID_USERNAME, "aaaa"),
                Arguments.of(VALID_USERNAME, "a".repeat(21)),
                Arguments.of(VALID_USERNAME, "abc 12")
        );
    }
}