package com.portfolio.faq;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FaqRetriever {
    private static final Set<String> STOP_WORDS = Set.of(
            "como", "qual", "quais", "para", "por", "que", "com", "uma", "uns", "das", "dos",
            "meu", "minha", "meus", "minhas", "seu", "sua", "isso", "essa", "esse", "esta", "tem",
            "posso", "pode", "voces", "veste", "loja", "camiseta", "camisetas", "quero", "gostaria",
            "saber", "sobre", "tenho", "preciso", "fazer", "sao", "ser", "estou", "ela", "ele");
    private final FaqRepository repository;

    public FaqRetriever(FaqRepository repository) { this.repository = repository; }

    public List<Match> retrieve(String question) {
        Set<String> query = tokens(question);
        List<Match> ranked = repository.findAll().stream()
                .map(faq -> new Match(faq, score(query, faq)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(Match::score).reversed().thenComparing(m -> m.faq().id()))
                .toList();
        if (ranked.isEmpty()) return List.of();
        int threshold = Math.max(2, (int) Math.ceil(ranked.getFirst().score() * 0.6));
        return ranked.stream().filter(m -> m.score() >= threshold).limit(3).toList();
    }

    private int score(Set<String> query, Faq faq) {
        Set<String> keywords = tokens(String.join(" ", faq.palavrasChave()));
        Set<String> title = tokens(faq.pergunta());
        return query.stream().mapToInt(token -> (keywords.contains(token) ? 3 : 0)
                + (title.contains(token) ? 2 : 0)).sum();
    }

    private Set<String> tokens(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t)).collect(Collectors.toSet());
    }

    public record Match(Faq faq, int score) {}
}
