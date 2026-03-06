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
    public String generateCode(CodeMigrationModel codeMigrationModel) {
        String prompt = buildPrompt(codeMigrationModel);

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
                    .uri(uriBuilder -> uriBuilder.path("/v1beta/models/gemini-3-flash-preview:generateContent")
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


    private String buildPrompt(CodeMigrationModel codeMigrationModel) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
You are an expert ABAP developer. Convert the following old syntax ABAP code to modern ABAP 7.4+ syntax.

Migration Rules:
1. Convert to modular structure with INCLUDE files (TOP, Selection Screen, Implementation)
2. Replace TYPE-POOLS with direct TYPES declarations
3. Convert internal table declarations to modern syntax: DATA : gt_table TYPE TABLE OF ty_structure
4. Replace FORM routines with CLASS methods
5. Use inline declarations: DATA(var), FIELD-SYMBOL(<fs>)
6. Modern SQL with @ host variables: SELECT fields FROM table AS alias INTO TABLE @data WHERE conditions
7. Replace REUSE_ALV_GRID_DISPLAY with CL_SALV_TABLE=>factory
8. Use LOOP AT...INTO DATA(ls_var) instead of LOOP AT...INTO wa
9. Replace manual field catalog building with automatic column optimization
10. Use TRY-CATCH for exception handling
11. Convert all MOVE statements to direct assignments or MOVE-CORRESPONDING
12. Preserve all business logic and calculations exactly
13. Return ONLY the migrated code with proper structure

Old Syntax ABAP Code:
""");

        prompt.append(codeMigrationModel.getCodeContent());
        prompt.append("\n\nProvide the complete migrated ABAP 7.4+ code with proper structure. Return only code, no explanations.");

        return prompt.toString();
    }
}
