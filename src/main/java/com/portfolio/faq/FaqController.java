package com.portfolio.faq;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FaqController {
    private final FaqService service;
    private final FaqRepository repository;

    public FaqController(FaqService service, FaqRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    @GetMapping("/faqs")
    public List<Faq> faqs() { return repository.findAll(); }

    @PostMapping("/chat")
    public FaqService.Answer chat(@Valid @RequestBody Question request) {
        return service.answer(request.pergunta());
    }

    public record Question(
            @NotBlank(message = "Informe uma pergunta.")
            @Size(max = 1000, message = "A pergunta deve ter até 1000 caracteres.") String pergunta) {}
}
