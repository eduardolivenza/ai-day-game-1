package com.game.dialog;

import com.game.util.Config;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that calls the AI_Day_Game_Server REST API.
 * POST /api/npc/chat  →  { npcMessage, choices[] }
 */
public class GameApiClient {

    public record ConversationResult(String npcMessage, List<String> choices) {}

    private final Config config;
    private final HttpClient http;

    public GameApiClient(Config config) {
        this.config = config;
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Start a fresh conversation (no playerMessage). Returns message + 3 choices. */
    public ConversationResult startConversation(String npcName,
                                                 List<Map<String, String>> history,
                                                 String vaultContext) {
        return call(npcName, history, null, true, vaultContext);
    }

    /**
     * Continue after player made a choice.
     * @param includeChoices true = typed reply (loop back), false = numbered choice (end).
     */
    public ConversationResult continueConversation(List<Map<String, String>> history,
                                                    String playerMessage,
                                                    boolean includeChoices,
                                                    String vaultContext) {
        return call(null, history, playerMessage, includeChoices, vaultContext);
    }

    // ── private ────────────────────────────────────────────────────────────

    private ConversationResult call(String npcName,
                                     List<Map<String, String>> history,
                                     String playerMessage,
                                     boolean includeChoices,
                                     String vaultContext) {
        try {
            JSONObject body = new JSONObject();
            if (npcName != null) body.put("npcName", npcName);
            body.put("includeChoices", includeChoices);
            if (playerMessage != null) body.put("playerMessage", playerMessage);
            if (vaultContext  != null && !vaultContext.isBlank())
                body.put("vaultContext", vaultContext);

            JSONArray hist = new JSONArray();
            for (Map<String, String> m : history) {
                JSONObject msg = new JSONObject();
                msg.put("role",    m.get("role"));
                msg.put("content", m.get("content"));
                hist.put(msg);
            }
            body.put("history", hist);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.getServerUrl() + "/api/npc/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.err.println("[Client] Server returned " + resp.statusCode());
                return fallback();
            }

            JSONObject json     = new JSONObject(resp.body());
            String message      = json.optString("npcMessage", "...");
            List<String> choices = new ArrayList<>();
            if (json.has("choices")) {
                JSONArray arr = json.getJSONArray("choices");
                for (int i = 0; i < arr.length(); i++)
                    choices.add(arr.getString(i));
            }
            return new ConversationResult(message, choices);

        } catch (java.net.ConnectException e) {
            return serverOffline();
        } catch (Exception e) {
            System.err.println("[Client] " + e.getMessage());
            return fallback();
        }
    }

    private ConversationResult serverOffline() {
        return new ConversationResult(
                "I cannot speak right now... "
                + "(Start the AI server: cd AI_Day_Game_Server && ./gradlew bootRun)",
                List.of("I understand.", "I will start it.", "Farewell."));
    }

    private ConversationResult fallback() {
        return new ConversationResult(
                "The winds carry strange whispers... try again.",
                List.of("I understand.", "I'll come back.", "Safe travels."));
    }
}
