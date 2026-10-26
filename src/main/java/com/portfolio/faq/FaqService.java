package com.portfolio.faq;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class FaqService {
    private final FaqRetriever retriever;
    private final GeminiClient gemini;
    private final GeminiProperties properties;

    public FaqService(FaqRetriever retriever, GeminiClient gemini, GeminiProperties properties) {
        this.retriever = retriever;
        this.gemini = gemini;
        this.properties = properties;
    }

    public Answer answer(String question) {
        var matches = retriever.retrieve(question);
        if (matches.isEmpty()) {
            return new Answer("Não encontrei essa informação nas perguntas frequentes. "
                    + "Entre em contato com atendimento@veste.example.", "SEM_CONTEXTO", false, List.of());
        }
        if (!properties.configured()) return local(matches.getFirst(), "BASE_LOCAL");
        try {
            String text = gemini.generate(question, matches.stream().map(FaqRetriever.Match::faq).toList());
            return new Answer(text, "GEMINI", true, matches.stream().map(Source::from).toList());
        } catch (RestClientException | IllegalStateException exception) {
            // Não registra exceções do provedor: podem conter conteúdo enviado pelo usuário.
            return local(matches.getFirst(), "FALLBACK_LOCAL");
        }
    }

    private Answer local(FaqRetriever.Match match, String mode) {
        return new Answer(match.faq().resposta(), mode, false, List.of(Source.from(match)));
    }

    public record Answer(String resposta, String modo, boolean usouGemini, List<Source> fontesRecuperadas) {}
    public record Source(String id, String pergunta, String categoria, String trecho, int pontuacao) {
        static Source from(FaqRetriever.Match match) {
            Faq faq = match.faq();
            return new Source(faq.id(), faq.pergunta(), faq.categoria(), faq.resposta(), match.score());
        }
    }
}
