package com.airepair.service;

import org.springframework.stereotype.Service;

@Service
public class QwenConfigService {
    private String apiKey;

    public synchronized void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public synchronized String getApiKey() {
        return this.apiKey;
    }
} 