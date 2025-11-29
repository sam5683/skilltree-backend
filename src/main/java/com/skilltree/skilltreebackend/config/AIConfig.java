package com.skilltree.skilltreebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Value("${GROQ_API_KEY:#{null}}")
    private String groqKey;

    @Value("${GEMINI_API_KEY:#{null}}")
    private String geminiKey;

    public String getGroqKey() {
        return groqKey;
    }

    public String getGeminiKey() {
        return geminiKey;
    }
}
