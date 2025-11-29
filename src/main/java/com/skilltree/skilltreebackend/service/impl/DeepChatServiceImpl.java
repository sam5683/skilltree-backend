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

@Service("deep")
public class DeepChatServiceImpl implements ChatService {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    private final AIConfig config;
    private final OkHttpClient client = new OkHttpClient();

    public DeepChatServiceImpl(AIConfig config) {
        this.config = config;
    }

    @Override
    public String chat(String node, String message) {
        String geminiKey = config.getGeminiKey();
        if (geminiKey == null || geminiKey.isBlank()) {
            return "Deep mode unavailable: GEMINI_API_KEY not configured.";
        }

        String bodyJson = String.format("""
            {
              "contents": [{
                "parts": [{
                  "text": "SkillTree Deep Mode. Node: %s\\nMessage: %s"
                }]
              }],
              "maxOutputTokens": 1200,
              "temperature": 0.2
            }
            """, safe(node), safe(message));

        HttpUrl url = HttpUrl.parse(API_URL).newBuilder()
                .addQueryParameter("key", geminiKey)
                .build();

        RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return "Empty response from Deep AI.";
            String json = response.body().string();

            // Gemini responses vary; try common extraction points
            return extractBestText(json).orElse("Deep AI returned no usable text.");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling Deep AI: " + e.getMessage();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    /**
     * Try multiple places commonly used by providers to return text.
     */
    private Optional<String> extractBestText(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return Optional.empty();
            JsonObject obj = root.getAsJsonObject();

            // 1) google-style: candidates or output->candidates
            if (obj.has("candidates") && obj.get("candidates").isJsonArray()) {
                JsonArray cands = obj.getAsJsonArray("candidates");
                if (cands.size() > 0) {
                    JsonObject first = cands.get(0).getAsJsonObject();
                    if (first.has("content")) return Optional.of(getAsString(first.get("content")));
                    if (first.has("text")) return Optional.of(getAsString(first.get("text")));
                }
            }

            // 2) maybe under "output" -> "contents" -> parts -> text
            if (obj.has("output") && obj.get("output").isJsonObject()) {
                JsonObject output = obj.getAsJsonObject("output");
                if (output.has("contents") && output.get("contents").isJsonArray()) {
                    JsonArray contents = output.getAsJsonArray("contents");
                    if (contents.size() > 0) {
                        JsonObject first = contents.get(0).getAsJsonObject();
                        if (first.has("text")) return Optional.of(getAsString(first.get("text")));
                        if (first.has("content")) return Optional.of(getAsString(first.get("content")));
                    }
                }
            }

            // 3) fallback to choices -> message -> content (OpenAI-like)
            if (obj.has("choices") && obj.get("choices").isJsonArray()) {
                JsonArray choices = obj.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject first = choices.get(0).getAsJsonObject();
                    if (first.has("message") && first.get("message").isJsonObject()) {
                        JsonObject msg = first.getAsJsonObject("message");
                        if (msg.has("content")) return Optional.of(getAsString(msg.get("content")));
                        if (msg.has("text")) return Optional.of(getAsString(msg.get("text")));
                    }
                    if (first.has("text")) return Optional.of(getAsString(first.get("text")));
                }
            }

            // 4) direct text/content fields
            if (obj.has("text")) return Optional.of(getAsString(obj.get("text")));
            if (obj.has("content")) return Optional.of(getAsString(obj.get("content")));

        } catch (Exception ex) {
            // ignore and fallback
        }
        return Optional.empty();
    }

    private static String getAsString(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) return el.getAsString();
        return el.toString();
    }
}
