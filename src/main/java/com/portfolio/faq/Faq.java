package com.portfolio.faq;

import java.util.List;

public record Faq(String id, String categoria, String pergunta, String resposta, List<String> palavrasChave) {}
