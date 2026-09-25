package com.todo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TodoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
    }

    @Test
    void 할_일_생성부터_조회_완료_처리_삭제까지_성공한다() throws Exception {
        String createRequest = objectMapper.writeValueAsString(
                Map.of(
                        "title", "Spring 과제하기",
                        "description", "통합 테스트 작성하기"
                )
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequest)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring 과제하기"))
                .andExpect(jsonPath("$.description").value("통합 테스트 작성하기"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(
                createResult.getResponse().getContentAsString()
        );
        long todoId = createResponse.get("id").asLong();

        mockMvc.perform(
                        get("/api/todos")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(todoId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/todos/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.title").value("Spring 과제하기"));

        String completionRequest = objectMapper.writeValueAsString(
                Map.of("completed", true)
        );

        mockMvc.perform(
                        patch("/api/todos/{id}/completed", todoId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(completionRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(
                        get("/api/todos")
                                .param("completed", "true")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].completed").value(true));

        mockMvc.perform(delete("/api/todos/{id}", todoId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/todos/{id}", todoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void 공백_제목은_400을_응답한다() throws Exception {
        String request = objectMapper.writeValueAsString(
                Map.of(
                        "title", "   ",
                        "description", "잘못된 Todo"
                )
        );

        mockMvc.perform(
                        post("/api/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path").value("/api/todos"))
                .andExpect(jsonPath("$.fieldErrors.title")
                        .value("제목은 비어 있거나 공백일 수 없습니다."));
    }

    @Test
    void 길이_상한을_넘는_제목은_400을_응답한다() throws Exception {
        String request = objectMapper.writeValueAsString(
                Map.of(
                        "title", "가".repeat(101),
                        "description", "잘못된 Todo"
                )
        );

        mockMvc.perform(
                        post("/api/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.title")
                        .value("제목은 100자 이하여야 합니다."));
    }

    @Test
    void 없는_ID를_조회하면_404를_응답한다() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("ID가 999999인 할 일을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/todos/999999"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void 없는_ID를_수정하면_404를_응답한다() throws Exception {
        String request = objectMapper.writeValueAsString(
                Map.of(
                        "title", "수정할 제목",
                        "description", "수정할 설명"
                )
        );

        mockMvc.perform(
                        put("/api/todos/{id}", 999999)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("ID가 999999인 할 일을 찾을 수 없습니다."));
    }

    @Test
    void 없는_ID를_삭제하면_404를_응답한다() throws Exception {
        mockMvc.perform(delete("/api/todos/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("ID가 999999인 할 일을 찾을 수 없습니다."));
    }
}