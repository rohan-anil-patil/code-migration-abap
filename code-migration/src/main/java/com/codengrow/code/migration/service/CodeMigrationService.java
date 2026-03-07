package com.codengrow.code.migration.service;

import com.codengrow.code.migration.model.CodeMigrationModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CodeMigrationService {

    private final WebClient webClient;
    private final String apiKey;

    public CodeMigrationService(WebClient.Builder webClientBuilder,
                                @Value("${gemini.api.url}") String baseUrl,
                                @Value("${gemini.api.key}") String geminiApiKey) {
        this.apiKey = geminiApiKey;
        this.webClient = webClientBuilder.baseUrl(baseUrl)
                .build();
    }
    public String generateCode(String codeContent) {
        String prompt = buildPrompt(codeContent);

        // construct request using ObjectMapper to ensure proper escaping
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode contentNode = mapper.createObjectNode();
        ObjectNode part = mapper.createObjectNode();
        part.put("text", prompt);
        contentNode.putArray("parts").add(part);
        root.putArray("contents").add(contentNode);

        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "Failed to construct request body: " + e.getMessage();
        }

        try {
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/v1beta/models/gemini-3.1-pro-preview:generateContent")
                            .build())
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractResponseContent(response);
        } catch (Exception ex) {
            // log error (if logger available) or just return message
            return "Error calling Gemini API: " + ex.getMessage();
        }
    }

    private String extractResponseContent(String response) {
        if (response == null) {
            return "No response from Gemini service";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error processing response: " + e.getMessage();
        }
    }


    private String buildPrompt(String codeContent) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
               You are an expert SAP ABAP developer with deep knowledge of ABAP 7.4+ syntax and Object-Oriented ABAP.
                
                               Your task is to convert legacy ABAP code into modern ABAP 7.4+ syntax while preserving the exact business logic.
                
                               Strict Rules:
                               1. Do NOT change the business logic.
                               2. Do NOT remove validations or conditions.
                               3. Do NOT modify variable meaning or program flow.
                               4. Only modernize syntax and refactor into Object-Oriented ABAP where possible.
                               5. If the input code is already using ABAP 7.4+ syntax, return the exact same code without changes.
                               6. Maintain identical output behavior.
                
                               Conversion Requirements:
                               - Replace old syntax with modern ABAP 7.4 syntax.
                               - Use inline declarations (DATA(...)).
                               - Use VALUE, NEW, CONV, REDUCE where applicable.
                               - Replace APPEND/LOOP patterns with modern expressions where safe.
                               - Replace READ TABLE with modern syntax using table expressions where possible.
                               - Use FIELD-SYMBOLS or DATA(...) inline where appropriate.
                               - Replace SELECT ... ENDSELECT with SELECT INTO TABLE or modern queries.
                               - Convert procedural FORM routines into CLASS methods.
                               - Wrap logic inside an Object-Oriented structure:
                                 - CLASS definition
                                 - PUBLIC/PRIVATE sections
                                 - METHODS
                
                               Output Format:
                               1. First show the converted ABAP 7.4+ Object-Oriented code.
                               2. Ensure the code compiles in ABAP 7.4 or higher.
                               3. Preserve comments if present.
                               4. Do not include explanations unless explicitly asked.
                
                               Input ABAP Code:
""");

        prompt.append(codeContent);
        prompt.append("\n\nProvide the complete migrated ABAP 7.4+ code with proper structure. Return only code, no explanations.");

        return prompt.toString();
    }
}
