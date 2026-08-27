package dev.gushchin.taskmanager.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ErrorPageControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownPageShouldShowStandaloneNotFoundPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/page-that-does-not-exist").with(user("user")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("class=\"error-page\"")))
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andExpect(content().string(containsString("Такая страница не найдена")))
                .andExpect(content().string(not(containsString("class=\"app-header\""))))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("TASKMEPLEASE"));
    }

    @Test
    void badRequestShouldShowStandaloneErrorPageWithoutLargeCode() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("class=\"error-page\"")))
                .andExpect(content().string(containsString("Не удалось обработать запрос")))
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Вернуться на главную"));
    }

    @Test
    void serverErrorShouldShowStandaloneErrorPageWithoutLargeCode() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("class=\"error-page\"")))
                .andExpect(content().string(containsString("Не удалось загрузить страницу")))
                .andExpect(content().string(containsString("Попробуйте обновить страницу немного позже.")))
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Вернуться на главную"));
    }

    @Test
    void apiFrameworkErrorShouldRemainJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/error")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Bad Request"));
    }
}
