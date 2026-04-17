package com.game.server.dto;

import java.util.ArrayList;
import java.util.List;

public class NpcChatRequest {

    private String npcName;
    private String systemPrompt;
    private List<MessageDto> history = new ArrayList<>();

    /** Null/blank = start a fresh conversation.  Non-blank = player's reply. */
    private String playerMessage;

    /**
     * true  → Claude should return a message + 3 choices (used on start and typed replies).
     * false → Claude should return a final message only (used after picking a numbered choice).
     */
    private boolean includeChoices = true;

    public String getNpcName()            { return npcName; }
    public String getSystemPrompt()       { return systemPrompt; }
    public List<MessageDto> getHistory()  { return history; }
    public String getPlayerMessage()      { return playerMessage; }
    public boolean isIncludeChoices()     { return includeChoices; }

    public void setNpcName(String v)               { npcName = v; }
    public void setSystemPrompt(String v)          { systemPrompt = v; }
    public void setHistory(List<MessageDto> v)     { history = v; }
    public void setPlayerMessage(String v)         { playerMessage = v; }
    public void setIncludeChoices(boolean v)       { includeChoices = v; }
}
