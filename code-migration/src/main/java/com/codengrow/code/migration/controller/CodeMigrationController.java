package com.codengrow.code.migration.controller;

import com.codengrow.code.migration.model.CodeMigrationModel;
import com.codengrow.code.migration.service.CodeMigrationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/code")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class CodeMigrationController {

    private final CodeMigrationService codeMigrationService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody CodeMigrationModel codeMigrationModel) {
        try {
            String response = codeMigrationService.generateCode(codeMigrationModel);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            // log if necessary
            return ResponseEntity.status(500).body("Internal error: " + ex.getMessage());
        }
    }
}
