package com.game.server.dto;

import java.util.ArrayList;
import java.util.List;

public class NpcChatRequest {

    private String npcName;
    private List<MessageDto> history = new ArrayList<>();

    /** Null/blank = start a fresh conversation.  Non-blank = player's reply. */
    private String playerMessage;

    /**
     * true  → Claude should return a message + 3 choices (used on start and typed replies).
     * false → Claude should return a final message only (used after picking a numbered choice).
     */
    private boolean includeChoices = true;

    /** Human-readable vault location description, injected into the NPC's system prompt. */
    private String vaultContext;

    public String getNpcName()            { return npcName; }
    public List<MessageDto> getHistory()  { return history; }
    public String getPlayerMessage()      { return playerMessage; }
    public boolean isIncludeChoices()     { return includeChoices; }
    public String getVaultContext()       { return vaultContext; }

    public void setNpcName(String v)               { npcName = v; }
    public void setHistory(List<MessageDto> v)     { history = v; }
    public void setPlayerMessage(String v)         { playerMessage = v; }
    public void setIncludeChoices(boolean v)       { includeChoices = v; }
    public void setVaultContext(String v)          { vaultContext = v; }
}
