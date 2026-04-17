package com.game.util;

import com.game.GamePanel;
import com.game.entity.NPC;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/** Saves and loads game state (player position, current map, NPC conversation history). */
public class SaveManager {

    private static final Path SAVE_FILE = Path.of("save.json");

    private final GamePanel gp;

    public SaveManager(GamePanel gp) {
        this.gp = gp;
    }

    public void saveGame() {
        try {
            JSONObject root = new JSONObject();
            root.put("currentMap",         gp.mapManager.getCurrentMap().getId());
            root.put("playerX",            gp.player.worldX);
            root.put("playerY",            gp.player.worldY);
            root.put("vaultLocationIndex", gp.vaultManager.getIndex());

            JSONObject conversations = new JSONObject();
            for (Map.Entry<String, NPC> entry : gp.mapManager.getNpcRegistry().entrySet()) {
                NPC npc = entry.getValue();
                if (npc.conversationHistory.isEmpty()) continue;

                JSONArray history = new JSONArray();
                for (Map<String, String> msg : npc.conversationHistory) {
                    JSONObject m = new JSONObject();
                    m.put("role",    msg.get("role"));
                    m.put("content", msg.get("content"));
                    history.put(m);
                }
                conversations.put(entry.getKey(), history);
            }
            root.put("npcConversations", conversations);

            Files.writeString(SAVE_FILE, root.toString(2));
            System.out.println("[Save] Game saved.");
        } catch (IOException e) {
            System.err.println("[Save] Failed to save: " + e.getMessage());
        }
    }

    public void loadGame() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            String json = Files.readString(SAVE_FILE);
            JSONObject root = new JSONObject(json);

            String mapId = root.optString("currentMap", "overworld");
            if (gp.mapManager.getMap(mapId) != null) {
                // Switch map without transition
                gp.mapManager.setCurrentMapDirect(mapId);
            }
            gp.player.worldX = root.optInt("playerX", 11 * 48);
            gp.player.worldY = root.optInt("playerY", 14 * 48);
            int vaultIdx = root.optInt("vaultLocationIndex", -1);
            if (vaultIdx >= 0) gp.vaultManager.setIndex(vaultIdx);

            JSONObject conversations = root.optJSONObject("npcConversations");
            if (conversations != null) {
                for (String npcName : conversations.keySet()) {
                    NPC npc = gp.mapManager.getNpcRegistry().get(npcName);
                    if (npc == null) continue;
                    npc.conversationHistory.clear();
                    JSONArray history = conversations.getJSONArray(npcName);
                    for (int i = 0; i < history.length(); i++) {
                        JSONObject m = history.getJSONObject(i);
                        Map<String, String> msg = new HashMap<>();
                        msg.put("role",    m.getString("role"));
                        msg.put("content", m.getString("content"));
                        npc.conversationHistory.add(msg);
                    }
                }
            }
            System.out.println("[Save] Game loaded.");
        } catch (Exception e) {
            System.err.println("[Save] Failed to load: " + e.getMessage());
        }
    }
}
