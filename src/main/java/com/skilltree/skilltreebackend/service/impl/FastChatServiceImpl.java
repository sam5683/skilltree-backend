package com.skilltree.skilltreebackend.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skilltree.skilltreebackend.config.AIConfig;
import com.skilltree.skilltreebackend.service.ChatService;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service("fast")
public class FastChatServiceImpl implements ChatService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final AIConfig config;
    private final OkHttpClient client = new OkHttpClient();

    public FastChatServiceImpl(AIConfig config) {
        this.config = config;
    }

    @Override
    public String chat(String node, String message) {
        String groqKey = config.getGroqKey();
        if (groqKey == null || groqKey.isBlank()) {
            return "Fast mode unavailable: GROQ_API_KEY not configured.";
        }

        String bodyJson = String.format("""
            {
              "model": "llama3-70b",
              "messages": [
                {"role": "system", "content": "You are SkillTree AI. Be clear, concise and educational."},
                {"role": "user", "content": "Node: %s\\nMessage: %s"}
              ],
              "max_tokens": 800,
              "temperature": 0.2
            }
            """, safe(node), safe(message));

        RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + groqKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return "Empty response from Fast AI.";
            String json = response.body().string();
            return extractBestText(json).orElse("Fast AI returned no usable text.");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling Fast AI: " + e.getMessage();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    /**
     * Robustly try to extract text from various chat-completion JSON shapes.
     */
    private Optional<String> extractBestText(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return Optional.empty();
            JsonObject obj = root.getAsJsonObject();

            // 1) Standard OpenAI-style: choices[0].message.content
            if (obj.has("choices") && obj.get("choices").isJsonArray()) {
                JsonArray choices = obj.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject first = choices.get(0).getAsJsonObject();
                    // message -> content
                    if (first.has("message") && first.get("message").isJsonObject()) {
                        JsonObject msg = first.getAsJsonObject("message");
                        if (msg.has("content")) return Optional.of(getAsString(msg.get("content")));
                        if (msg.has("text")) return Optional.of(getAsString(msg.get("text")));
                    }
                    // text (older APIs)
                    if (first.has("text")) return Optional.of(getAsString(first.get("text")));
                }
            }

            // 2) direct field "text" or "content"
            if (obj.has("text")) return Optional.of(getAsString(obj.get("text")));
            if (obj.has("content")) return Optional.of(getAsString(obj.get("content")));

            // 3) Try nested candidates array (some providers)
            if (obj.has("candidates") && obj.get("candidates").isJsonArray()) {
                JsonArray cands = obj.getAsJsonArray("candidates");
                if (cands.size() > 0) {
                    JsonObject first = cands.get(0).getAsJsonObject();
                    if (first.has("content")) return Optional.of(getAsString(first.get("content")));
                }
            }
        } catch (Exception ex) {
            // fallthrough
        }
        return Optional.empty();
    }

    private static String getAsString(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        return el.toString();
    }
}
