package com.airepair.service;

import com.airepair.entity.Model;
import com.airepair.repository.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModelService {

    @Autowired
    private ModelRepository modelRepository;

    @Transactional(readOnly = true)
    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    @Transactional
    public Model addModel(String name, String apiKey) {
        if (modelRepository.existsByName(name)) {
            throw new RuntimeException("Model already exists: " + name);
        }

        Model model = new Model();
        model.setName(name);
        model.setApiKey(apiKey);
        return modelRepository.save(model);
    }

    @Transactional
    public void deleteModel(String name) {
        Model model = modelRepository.findByName(name)
            .orElseThrow(() -> new RuntimeException("Model not found: " + name));
        modelRepository.delete(model);
    }

    @Transactional
    public void deleteModelById(Long id) {
        if (!modelRepository.existsById(id)) {
            throw new RuntimeException("Model not found with id: " + id);
        }
        modelRepository.deleteById(id);
    }

    @Transactional
    public Model updateModelApiKey(String name, String apiKey) {
        Model model = modelRepository.findByName(name)
            .orElseThrow(() -> new RuntimeException("Model not found: " + name));
        model.setApiKey(apiKey);
        return modelRepository.save(model);
    }
}
