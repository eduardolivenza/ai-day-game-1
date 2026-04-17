package com.game.server.dto;

import java.util.ArrayList;
import java.util.List;

public class NpcChatResponse {

    private String npcMessage;
    private List<String> choices = new ArrayList<>();

    public NpcChatResponse() {}
    public NpcChatResponse(String npcMessage, List<String> choices) {
        this.npcMessage = npcMessage;
        this.choices    = choices != null ? choices : new ArrayList<>();
    }

    public String getNpcMessage()      { return npcMessage; }
    public List<String> getChoices()   { return choices; }
    public void setNpcMessage(String v)      { npcMessage = v; }
    public void setChoices(List<String> v)   { choices = v; }
}
