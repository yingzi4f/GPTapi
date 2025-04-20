package com.airepair.controller;

import com.airepair.service.QwenConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qwen-config")
@CrossOrigin
public class QwenConfigController {
    @Autowired
    private QwenConfigService qwenConfigService;

    @GetMapping("/key")
    public ResponseEntity<String> getApiKey() {
        String key = qwenConfigService.getApiKey();
        if (key == null || key.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(key);
    }

    @PostMapping("/key")
    public ResponseEntity<Void> setApiKey(@RequestBody String apiKey) {
        qwenConfigService.setApiKey(apiKey);
        return ResponseEntity.ok().build();
    }
} 