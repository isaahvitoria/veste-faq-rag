package com.portfolio.faq;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Repository
public class FaqRepository {
    private final List<Faq> faqs;

    public FaqRepository(JsonMapper mapper) throws IOException {
        try (var input = new ClassPathResource("knowledge/faqs.json").getInputStream()) {
            faqs = List.copyOf(Arrays.asList(mapper.readValue(input, Faq[].class)));
        }
        if (faqs.isEmpty() || faqs.stream().map(Faq::id).distinct().count() != faqs.size()) {
            throw new IllegalStateException("A base deve conter FAQs com IDs únicos.");
        }
    }

    public List<Faq> findAll() { return faqs; }
}
