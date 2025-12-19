package com.williamcallahan.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.models.Model;
import com.williamcallahan.Config;
import com.williamcallahan.ConfigException;

import java.util.List;

/** Shared OpenAI client configuration and utilities. */
public final class OpenAiService {

    private final OpenAIClient client;
    private final String baseUrl;

    public OpenAiService(Config config) {
        String apiKey = config.resolveApiKey();
        String customBaseUrl = System.getenv("OPENAI_BASE_URL");

        if (apiKey == null) {
            String target = (customBaseUrl == null || customBaseUrl.isBlank())
                ? "the OpenAI API (https://api.openai.com/v1)"
                : customBaseUrl;
            throw new ConfigException("""

                ┌─────────────────────────────────────────────────────────────────┐
                │  Missing API Key                                                │
                └─────────────────────────────────────────────────────────────────┘

                No API key found in environment (OPENAI_API_KEY) or config file.

                Target endpoint: %s

                Quick fix (choose one):
                  • Set environment variable:  export OPENAI_API_KEY=your-key
                  • Or run the app and enter your key when prompted

                Get an API key:
                  → https://platform.openai.com/docs/quickstart

                For alternative providers (OpenRouter, LMStudio, Ollama):
                  → docs/environment-variables-api-keys.md

                """.formatted(target));
        }

        // Build client with resolved API key (env or config) — empty string is valid for some endpoints
        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(customBaseUrl != null && !customBaseUrl.isBlank()
                ? customBaseUrl
                : "https://api.openai.com/v1")
            .build();
        this.baseUrl = customBaseUrl;
    }

    public OpenAIClient client() {
        return client;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public List<String> modelChoices() {
        return client.models().list().autoPager().stream()
            .map(Model::id)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .toList();
    }
}
