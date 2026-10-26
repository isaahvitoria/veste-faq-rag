package com.portfolio.faq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FaqServiceTest {
    private FaqRetriever retriever;
    private GeminiClient gemini;
    private FaqService service;

    @BeforeEach
    void setup() throws Exception {
        retriever = new FaqRetriever(new FaqRepository(JsonMapper.builder().build()));
        gemini = mock(GeminiClient.class);
        service = new FaqService(retriever, gemini, new GeminiProperties("fake-test-key", "test-model"));
    }

    @Test
    void ranksAccentedAndUppercaseQuestion() {
        assertThat(retriever.retrieve("Como funciona o REEMBOLSO? ESTORNO?"))
                .first().extracting(m -> m.faq().id()).isEqualTo("devolucao-02");
    }

    @Test
    void doesNotRetrieveFromGenericStoreWords() {
        assertThat(retriever.retrieve("Quero saber sobre a loja de camisetas")).isEmpty();
    }

    @Test
    void passesRetrievedFaqsToGeminiAndReturnsSources() {
        when(gemini.generate(eq("Qual o tecido?"), anyList())).thenReturn("A linha básica usa algodão [produto-03].");
        var answer = service.answer("Qual o tecido?");
        assertThat(answer.modo()).isEqualTo("GEMINI");
        assertThat(answer.usouGemini()).isTrue();
        assertThat(answer.fontesRecuperadas()).extracting(FaqService.Source::id).containsExactly("produto-03");
        verify(gemini).generate(eq("Qual o tecido?"), argThat(faqs -> faqs.size() == 1
                && faqs.getFirst().id().equals("produto-03")));
    }

    @Test
    void providerFailureReturnsActualFaqWithExplicitFallbackMode() {
        when(gemini.generate(anyString(), anyList())).thenThrow(new RestClientException("timeout"));
        var answer = service.answer("Qual o tecido?");
        assertThat(answer.modo()).isEqualTo("FALLBACK_LOCAL");
        assertThat(answer.usouGemini()).isFalse();
        assertThat(answer.resposta()).contains("100% algodão");
    }

    @Test
    void noContextNeverCallsGeminiEvenWithKeyConfigured() {
        assertThat(service.answer("Explique buracos negros").modo()).isEqualTo("SEM_CONTEXTO");
        verifyNoInteractions(gemini);
    }
}
