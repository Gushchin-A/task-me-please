package dev.gushchin.taskmanager.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
@WithMockUser
class UserControllerIntegrationTest extends IntegrationTestBase {
    private static final String BASE_URL = "/users";
    private static final String CREATE_USER = "request-json/create-user/create-user.json";
    private static final String INVALID_EMAIL = "request-json/create-user/create-user-invalid-email.json";
    private static final String BLANK_PASSWORD = "request-json/create-user/create-user-blank-password.json";
    private static final String BLANK_PASSWORD_SPACE = "request-json/create-user/create-user-blank-password_space.json";
    private static final String NULL_PASSWORD = "request-json/create-user/create-user-null-password.json";

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void findAllReturnStatusOkAndJson() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createShouldReturnCreatedUser() throws Exception {
        String requestBody = readJson(CREATE_USER);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("mike@test.com"))
                .andExpect(jsonPath("$.name").value("mike@test.com"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getUserReturnCreatedUser() throws Exception {
        String requestBody = readJson(CREATE_USER);

        String response = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("mike@test.com"))
                .andExpect(jsonPath("$.name").value("mike@test.com"));
    }

    @Test
    void getUserReturnNotFoundWhenUserNotExist() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void createShouldReturnConflictWhenEmailExists() throws Exception {
        String requestBody = readJson(CREATE_USER);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void createReturnBadRequestWhenInvalidEmail() throws Exception {
        String requestBody = readJson(INVALID_EMAIL);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenPasswordIsBlank() throws Exception {
        String requestBody = readJson(BLANK_PASSWORD);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenPasswordIsBlankSpace() throws Exception {
        String requestBody = readJson(BLANK_PASSWORD_SPACE);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenPasswordIsNull() throws Exception {
        String requestBody = readJson(NULL_PASSWORD);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }
}
