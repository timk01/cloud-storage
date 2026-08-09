package storage.cloud.cloudstorage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import storage.cloud.cloudstorage.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private UserService userService;

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

        HttpSession session = result.getRequest().getSession(false); //getSession(false) will return current session if current session exists. If not, it will not create a new session.
        //или, с флагом ТРУ - или без него - оно создаст новую сессию, если ее там нет.
        //а нам надо просто првоерить что ее удалили (нет сессии - и ок)

        assertThat(session).isNull();
    }

    @Test
    public void loggingUserFailedIfItHasNoSession() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

}