package com.portfolio.faq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "gemini.api-key=")
@AutoConfigureMockMvc
class FaqApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean GeminiClient gemini;

    @Test
    void servesKnowledgeBase() throws Exception {
        mvc.perform(get("/api/faqs")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(25)));
    }

    @Test
    void answersParaphraseWithTraceableSourceWithoutApiKey() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"Comprei uma camiseta pequena, consigo trocar?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("BASE_LOCAL"))
                .andExpect(jsonPath("$.usouGemini").value(false))
                .andExpect(jsonPath("$.fontesRecuperadas[0].id").value("trocas-01"));
        verifyNoInteractions(gemini);
    }

    @Test
    void refusesUnrelatedQuestionWithoutCallingProvider() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"Qual a capital da França?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modo").value("SEM_CONTEXTO"))
                .andExpect(jsonPath("$.fontesRecuperadas").isEmpty());
        verifyNoInteractions(gemini);
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"   \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOversizedQuestion() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
