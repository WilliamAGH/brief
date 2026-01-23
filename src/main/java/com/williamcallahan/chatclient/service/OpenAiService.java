package com.williamcallahan.chatclient.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.models.Model;
import com.williamcallahan.chatclient.Config;
import com.williamcallahan.chatclient.ConfigException;

import java.util.List;

/** Shared OpenAI client configuration and utilities. */
public final class OpenAiService {

    private final OpenAIClient client;
    private final String baseUrl;

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    public OpenAiService(Config config) {
        String apiKey = config.resolveApiKey();
        String resolvedBaseUrl = config.resolveBaseUrl();

        if (apiKey == null) {
            String target = (resolvedBaseUrl == null)
                ? "the OpenAI API (" + DEFAULT_BASE_URL + ")"
                : resolvedBaseUrl;
            throw new ConfigException("""

                ┌─────────────────────────────────────────────────────────────────┐
                │  Missing API Key                                                │
                └─────────────────────────────────────────────────────────────────┘

                No API key found in config file or environment.

                Target endpoint: %s

                For users (Homebrew install):
                  • Run the app normally — it will prompt for your key
                  • Key is saved to: ~/.config/brief/config

                For developers (local build):
                  • Copy .env-example to .env and add your key
                  • Or: export OPENAI_API_KEY=your-key

                Get an API key:
                  → https://platform.openai.com/docs/quickstart

                For alternative providers (OpenRouter, LMStudio, Ollama):
                  → docs/environment-variables-api-keys.md

                """.formatted(target));
        }

        // Build client with resolved API key and base URL (env > config > default)
        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl != null ? resolvedBaseUrl : DEFAULT_BASE_URL)
            .build();
        this.baseUrl = resolvedBaseUrl;
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
