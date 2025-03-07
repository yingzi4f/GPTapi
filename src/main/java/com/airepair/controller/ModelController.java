package com.airepair.controller;

import com.airepair.entity.Model;
import com.airepair.service.ModelService;
import com.airepair.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/models")
@CrossOrigin
public class ModelController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private OllamaService ollamaService;

    @GetMapping("/local")
    public ResponseEntity<List<String>> getLocalModels() {
        try {
            List<String> models = ollamaService.getLocalModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to get local models", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Model>> getSavedModels() {
        try {
            List<Model> models = modelService.getAllModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to get saved models", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> addModel(@RequestBody Map<String, String> request) {
        try {
            String modelName = request.get("name");
            if (modelName == null || modelName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Model name cannot be empty");
            }
            
            Model model = modelService.addModel(modelName);
            return ResponseEntity.ok(model);
        } catch (Exception e) {
            log.error("Failed to add model", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable Long id) {
        try {
            modelService.deleteModelById(id);
            return ResponseEntity.ok().body("Model deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete model", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<?> deleteModelByName(@PathVariable String name) {
        try {
            modelService.deleteModel(name);
            return ResponseEntity.ok().body("Model deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete model", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
