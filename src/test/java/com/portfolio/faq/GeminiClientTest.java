package com.portfolio.faq;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiClientTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GeminiClient client = new GeminiClient(new GeminiProperties("test-key", "test-model"),
            JsonMapper.builder().build(), builder.build());

    @Test
    void sendsRulesSeparatelyAndExcludesThoughtPartsFromAnswer() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").exists())
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("produto-03")))
                .andRespond(withSuccess("""
                        {"candidates":[{"finishReason":"STOP","content":{"parts":[
                          {"thought":true,"text":"raciocínio interno"},
                          {"text":"A linha básica usa algodão [produto-03]."}
                        ]}}]}
                        """, MediaType.APPLICATION_JSON));
        var faq = new Faq("produto-03", "produto", "Qual o tecido?", "Algodão.", List.of("tecido"));
        assertThat(client.generate("Qual o tecido?", List.of(faq)))
                .isEqualTo("A linha básica usa algodão [produto-03].");
        server.verify();
    }

    @Test
    void rejectsBlockedResponseInsteadOfReturningEmptySuccess() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.generate("pergunta", List.of()))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }
}
