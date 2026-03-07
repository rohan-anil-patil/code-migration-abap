package com.codengrow.code.migration.controller;

import com.codengrow.code.migration.service.CodeMigrationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("api/code")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class CodeMigrationController {

    private final CodeMigrationService codeMigrationService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestParam("text") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            
            String codeContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String response = codeMigrationService.generateCode(codeContent);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Internal error: " + ex.getMessage());
        }
    }
}
