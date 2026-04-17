package com.game.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "npc")
public class NpcProperties {

    private String globalBackground = "";
    private Map<String, NpcConfig> characters = new HashMap<>();

    public static class NpcConfig {
        private String context = "";

        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }

    /** Assembles the full system prompt for a given NPC (case-insensitive name match). */
    public String buildSystemPrompt(String npcName) {
        String context = characters.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(npcName))
            .map(e -> e.getValue().getContext())
            .findFirst()
            .orElse("");
        return globalBackground + (context.isBlank() ? "" : "\n\n" + context);
    }

    public String getGlobalBackground() { return globalBackground; }
    public void setGlobalBackground(String globalBackground) { this.globalBackground = globalBackground; }

    public Map<String, NpcConfig> getCharacters() { return characters; }
    public void setCharacters(Map<String, NpcConfig> characters) { this.characters = characters; }
}
