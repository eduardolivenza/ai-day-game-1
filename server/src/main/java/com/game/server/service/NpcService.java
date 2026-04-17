package com.game.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.server.config.NpcProperties;
import com.game.server.dto.MessageDto;
import com.game.server.dto.NpcChatRequest;
import com.game.server.dto.NpcChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NpcService {

    private static final Logger log = LoggerFactory.getLogger(NpcService.class);

    private final ChatModel     chatModel;
    private final ObjectMapper  objectMapper;
    private final NpcProperties npcProperties;

    public NpcService(ChatModel chatModel, ObjectMapper objectMapper, NpcProperties npcProperties) {
        this.chatModel      = chatModel;
        this.objectMapper   = objectMapper;
        this.npcProperties  = npcProperties;
    }

    public NpcChatResponse chat(NpcChatRequest req) {
        String systemPrompt = npcProperties.buildSystemPrompt(req.getNpcName());
        if (req.getVaultContext() != null && !req.getVaultContext().isBlank()) {
            systemPrompt += "\n\nYou know exactly where the ancient vault is hidden: "
                    + req.getVaultContext() + ". "
                    + "If the traveler asks about the vault, treasure, or hidden things, "
                    + "give a clue about its location in your own style and personality — "
                    + "poetic, cryptic, or direct depending on who you are. "
                    + "Do not simply state the location outright; let your personality shape the hint.";
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Rebuild conversation history
        for (MessageDto m : req.getHistory()) {
            if ("assistant".equals(m.getRole())) {
                messages.add(new AssistantMessage(m.getContent()));
            } else {
                messages.add(new UserMessage(m.getContent()));
            }
        }

        messages.add(new UserMessage(buildUserContent(req)));

        try {
            Prompt prompt = new Prompt(messages);
            ChatResponse response  = chatModel.call(prompt);
            String raw = response.getResult().getOutput().getText().trim();
            return parseResponse(raw, req.isIncludeChoices());
        } catch (Exception e) {
            log.error("ChatModel call failed", e);
            return fallback();
        }
    }

    // ── prompt construction ───────────────────────────────────────────────

    private String buildUserContent(NpcChatRequest req) {
        boolean isStart = req.getPlayerMessage() == null || req.getPlayerMessage().isBlank();

        if (isStart) {
            return req.isIncludeChoices()
                ? "A traveler approaches you. Greet them and ask something interesting. "
                  + "Provide exactly 3 short reply options for the traveler. "
                  + "Reply ONLY with valid JSON, no extra text:\n"
                  + "{\"message\":\"<npc dialog>\",\"choices\":[\"<opt1>\",\"<opt2>\",\"<opt3>\"]}"
                : "A traveler approaches you. Greet them briefly. "
                  + "Reply ONLY with valid JSON: {\"message\":\"<npc greeting>\"}";
        }

        String escaped = req.getPlayerMessage().replace("\"", "\\\"");
        return req.isIncludeChoices()
            ? "The traveler says: \"" + escaped + "\". "
              + "Respond in character and provide 3 new short reply options. "
              + "Reply ONLY with valid JSON:\n"
              + "{\"message\":\"<npc response>\",\"choices\":[\"<opt1>\",\"<opt2>\",\"<opt3>\"]}"
            : "The traveler says: \"" + escaped + "\". "
              + "Respond in character in 2-3 sentences. "
              + "Reply ONLY with valid JSON: {\"message\":\"<npc response>\"}";
    }

    // ── response parsing ──────────────────────────────────────────────────

    private NpcChatResponse parseResponse(String raw, boolean expectChoices) {
        // Strip optional markdown code fence
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("(?m)```\\s*$", "").trim();
        }
        try {
            JsonNode node    = objectMapper.readTree(raw);
            String message   = node.path("message").asText("...");
            List<String> choices = new ArrayList<>();
            if (expectChoices && node.has("choices")) {
                JsonNode arr = node.get("choices");
                for (int i = 0; i < Math.min(3, arr.size()); i++)
                    choices.add(arr.get(i).asText());
            }
            return new NpcChatResponse(message, choices);
        } catch (Exception e) {
            log.warn("Could not parse AI response as JSON: {}", raw);
            // Return the raw text if JSON parsing fails
            return new NpcChatResponse(raw.length() > 300 ? raw.substring(0, 300) : raw,
                                       new ArrayList<>());
        }
    }

    private NpcChatResponse fallback() {
        return new NpcChatResponse(
            "The winds carry strange whispers today... I cannot speak clearly.",
            List.of("I understand.", "I'll come back.", "Safe travels."));
    }
}
