package dev.gushchin.taskmanager.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
@WithMockUser
class TeamControllerIntegrationTest extends IntegrationTestBase {
    private static final String USERS_URL = "/users";
    private static final String TEAMS_URL = "/teams";
    private static final String CREATE_USER = "request-json/create-user/create-user.json";
    private static final String CREATE_TEAM = "request-json/create-team/create-team.json";
    private static final String INVALID_CREATED_BY = "request-json/create-team/create-team-invalid-created-by.json";
    private static final String BLANK_NAME = "request-json/create-team/create-team-blank-name.json";
    private static final String BLANK_NAME_SPACE = "request-json/create-team/create-team-blank-name-space.json";
    private static final String NULL_CREATED_BY = "request-json/create-team/create-team-null-created-by.json";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @BeforeEach
    void setUp() {
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findAllReturnStatusOkAndArrayJson() throws Exception {
        mockMvc.perform(get(TEAMS_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createTeamReturnCreatedTeam() throws Exception {
        String userId = createUserAndReturnId();

        String requestBody = readJson(CREATE_TEAM).replace("{{createdBy}}", userId);

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Chukaripa"))
                .andExpect(jsonPath("$.createdBy").value(userId))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getTeamReturnCreatedTeam() throws Exception {
        String userId = createUserAndReturnId();

        String createTeamRequest = readJson(CREATE_TEAM).replace("{{createdBy}}", userId);

        String response = mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeamRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number tempTeamId = JsonPath.read(response, "$.id");
        Long teamId = tempTeamId.longValue();

        mockMvc.perform(get(TEAMS_URL + "/" + teamId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(teamId))
                .andExpect(jsonPath("$.name").value("Chukaripa"))
                .andExpect(jsonPath("$.createdBy").value(userId));
    }

    private String createUserAndReturnId() throws Exception {
        String requestBody = readJson(CREATE_USER);

        String response = mockMvc.perform(post(USERS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.id");
    }

    @Test
    void getTeamReturnNotFoundWhenTeamNotExist() throws Exception {
        mockMvc.perform(get(TEAMS_URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TEAM_NOT_FOUND"));
    }

    @Test
    void createReturnNotFoundWhenCreatorNotExist() throws Exception {
        String requestBody =
                readJson(CREATE_TEAM).replace("{{createdBy}}", UUID.randomUUID().toString());

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void createReturnBadRequestWhenCreatedByInvalid() throws Exception {
        String requestBody = readJson(INVALID_CREATED_BY);

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenNameIsBlank() throws Exception {
        String userId = createUserAndReturnId();
        String requestBody = readJson(BLANK_NAME).replace("{{createdBy}}", userId);

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenNameIsBlankSpace() throws Exception {
        String userId = createUserAndReturnId();
        String requestBody = readJson(BLANK_NAME_SPACE).replace("{{createdBy}}", userId);

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createReturnBadRequestWhenCreatedByIsNull() throws Exception {
        String requestBody = readJson(NULL_CREATED_BY);

        mockMvc.perform(post(TEAMS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }
}
