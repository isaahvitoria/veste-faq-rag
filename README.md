# Veste — FAQ RAG

API de perguntas frequentes de uma **loja fictícia de camisetas**, construída com Java 21, Spring Boot e apoio do Gemini. Projeto de portfólio: as políticas e os contatos são exemplos, sem ligação com uma loja real.

A API recupera FAQs de um arquivo JSON e envia somente os registros selecionados ao Gemini para formular a resposta. As referências recuperadas acompanham o resultado. Sem chave de API, é possível experimentar a busca com respostas diretas da base.

## Arquitetura

```text
POST /api/chat → validação → busca lexical nas 25 FAQs
                              ├─ sem resultado → orientação para atendimento
                              ├─ sem chave → resposta direta da FAQ mais relevante
                              └─ com chave → Gemini → resposta + fontes recuperadas
                                              └─ falha → resposta direta da FAQ
```

A busca normaliza caixa e acentos, remove palavras genéricas e pontua correspondências na pergunta e nas palavras-chave. Recupera até três FAQs com ao menos 60% da pontuação da primeira. Cada FAQ já representa um trecho independente: não é necessário dividir PDFs nem usar banco de dados nesta versão.

O fluxo de recuperação + geração constitui o RAG desta versão. A recuperação é lexical, sem embeddings. O modo local apenas recupera e retorna uma resposta cadastrada, sem geração por IA.

## Executar no Windows

Requisito: JDK 21 com `JAVA_HOME` configurado. O Maven Wrapper baixa o Maven no primeiro uso; Maven e dependências precisam de acesso à internet nessa etapa.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS: `bash mvnw test` e `bash mvnw spring-boot:run`.

A API inicia em `http://localhost:8080`. Não há interface gráfica nesta versão.

### Habilitar o Gemini

Crie uma chave no [Google AI Studio](https://aistudio.google.com/apikey) e configure na mesma janela do PowerShell antes de iniciar a API:

```powershell
$env:GEMINI_API_KEY = 'sua-chave'
$env:GEMINI_MODEL = 'gemini-3.8-flash'
.\mvnw.cmd spring-boot:run
```

O modelo é configurável: utilize um modelo `generateContent` disponível para sua conta. O uso da API está sujeito às cotas e cobranças do provedor. A chave é enviada no cabeçalho `x-goog-api-key`, nunca na URL. Conexão limitada a 5 segundos e leitura a 30 segundos.

O arquivo `.env.example` documenta as variáveis; Spring Boot **não carrega `.env` automaticamente**. Para executar via Maven, use as variáveis de ambiente acima. Nunca faça commit da chave.

### Docker

```powershell
docker compose up --build
```

O Compose usa as variáveis do terminal ou um `.env` local, que está ignorado pelo Git. A aplicação fica disponível apenas no loopback do host pela configuração padrão do Compose.

## Consultar

```powershell
$body = @{ pergunta = 'Comprei uma camiseta pequena, consigo trocar?' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8080/api/chat' -Method Post `
  -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
```

Exemplo sem chave:

```json
{
  "resposta": "Na política fictícia da Veste, a troca por tamanho pode ser solicitada em até 30 dias corridos após o recebimento, com a peça sem uso e com etiqueta. A disponibilidade do novo tamanho será confirmada pelo atendimento.",
  "modo": "BASE_LOCAL",
  "usouGemini": false,
  "fontesRecuperadas": [{
    "id": "trocas-01",
    "pergunta": "Posso trocar uma peça por outro tamanho?",
    "categoria": "trocas",
    "trecho": "Na política fictícia da Veste, a troca por tamanho pode ser solicitada em até 30 dias corridos após o recebimento, com a peça sem uso e com etiqueta. A disponibilidade do novo tamanho será confirmada pelo atendimento.",
    "pontuacao": 8
  }]
}
```

| Endpoint | Função |
| --- | --- |
| `GET /api/health` | Verifica se a aplicação está disponível |
| `GET /api/faqs` | Lista a base pública de demonstração |
| `POST /api/chat` | Recebe `{"pergunta":"..."}` e retorna resposta e fontes |

Pergunta obrigatória, com até 1.000 caracteres. Entradas inválidas retornam HTTP 400 com Problem Detail.

| Modo | Significado |
| --- | --- |
| `GEMINI` | Resposta gerada com os registros recuperados |
| `BASE_LOCAL` | Sem chave: resposta cadastrada da primeira FAQ |
| `FALLBACK_LOCAL` | Falha, bloqueio ou resposta incompleta do provedor: resposta da primeira FAQ |
| `SEM_CONTEXTO` | Nenhuma correspondência: não chama o Gemini |

`fontesRecuperadas` registra os documentos fornecidos como contexto, sem garantir que cada um foi utilizado pelo modelo. `pontuacao` é uma contagem ponderada de palavras, **não uma probabilidade de confiança**.

## Personalizar a base

Edite [`src/main/resources/knowledge/faqs.json`](src/main/resources/knowledge/faqs.json) e reinicie a aplicação. Cada registro contém `id` único, `categoria`, `pergunta`, `resposta` e `palavrasChave`. Inclua variações comuns nas palavras-chave para melhorar a recuperação.

Exemplos para experimentar: “Qual o tecido?”, “Como funciona o estorno?”, “Aceitam boleto?”, “Como acompanho o rastreio?” e “Qual a capital da França?” (sem contexto).

## Validação e limitações

`mvnw test` verifica recuperação com paráfrase, acentos, ausência de contexto, integração HTTP, validação de entrada e recuperação após falha do provedor. Os testes usam a base real de exemplo e um cliente Gemini simulado; não gastam cota e não validam a qualidade de respostas do Gemini real.

Esta versão não mantém histórico, não consulta pedidos ou estoque e não executa operações de compra. A busca por palavras pode perder sinônimos e recuperar contextos insuficientes, especialmente em perguntas com várias intenções. No modo local, apenas a FAQ mais relevante é respondida. As regras de fidelidade às fontes e resistência a comandos na pergunta são instruções ao modelo, não uma garantia técnica contra alucinações.

A API inicial não possui autenticação nem limite de requisições: execute localmente. Uma publicação com chave do provedor exige controle de acesso e cotas para limitar consumo. Não envie dados pessoais no projeto de demonstração.

Evoluções possíveis: avaliação com perguntas e fontes esperadas, busca semântica, comparação com a busca lexical e cache de perguntas frequentes.

Referências: [Gemini generateContent](https://ai.google.dev/api/generate-content), [chaves da API](https://ai.google.dev/gemini-api/docs/api-key) e [Spring Boot](https://docs.spring.io/spring-boot/).
