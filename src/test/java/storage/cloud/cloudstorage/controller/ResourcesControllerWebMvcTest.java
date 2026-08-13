package storage.cloud.cloudstorage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import storage.cloud.cloudstorage.service.ResourcesService;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourcesController.class)
class ResourcesControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private ResourcesService resourcesService;


    @ParameterizedTest
    @MethodSource("invalidPath")
    public void folderCreationFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(post("/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @MethodSource("invalidPath")
    public void gettingFolderFailedDueToInvalidPath(String path) throws Exception {
        mockMvc.perform(get("/directory")
                        .sessionAttr("userId", 1L)
                        .param("path", path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private static Stream<Arguments> invalidPath() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("№№№/"),
                Arguments.of("abc")
        );
    }

    /**
     * No paths at all!
     *
     * @throws Exception
     */

    @Test
    public void folderCreationFailedDueToNoPath() throws Exception {
        mockMvc.perform(post("/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void gettingFolderFailedDueToInvalidPath() throws Exception {
        mockMvc.perform(get("/directory")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}