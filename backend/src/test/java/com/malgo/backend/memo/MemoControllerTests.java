package com.malgo.backend.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class MemoControllerTests {

    private MemoService memoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        memoService = org.mockito.Mockito.mock(MemoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MemoController(memoService)).build();
    }

    @Test
    void createsMemoFromPostmanPayload() throws Exception {
        org.mockito.Mockito.when(memoService.create(org.mockito.ArgumentMatchers.any(MemoCreateRequest.class)))
                .thenReturn(new MemoResponse(1L, "postman", "hello memo", LocalDateTime.now()));

        mockMvc.perform(post("/api/memo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "postman",
                                  "memo": "hello memo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("postman"))
                .andExpect(jsonPath("$.content").value("hello memo"));
    }

    @Test
    void rejectsEmptyMemoPayload() throws Exception {
        org.mockito.Mockito.when(memoService.create(org.mockito.ArgumentMatchers.any(MemoCreateRequest.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/memo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsMemos() throws Exception {
        org.mockito.Mockito.when(memoService.findAll())
                .thenReturn(List.of(new MemoResponse(1L, "postman", "hello memo", LocalDateTime.now())));

        mockMvc.perform(get("/api/memo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("hello memo"));
    }
}
