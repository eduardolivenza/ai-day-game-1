package com.game.server.controller;

import com.game.server.dto.NpcChatRequest;
import com.game.server.dto.NpcChatResponse;
import com.game.server.service.NpcService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/npc")
@CrossOrigin   // allow calls from the game process on the same machine
public class NpcController {

    private final NpcService npcService;

    public NpcController(NpcService npcService) {
        this.npcService = npcService;
    }

    @PostMapping("/chat")
    public NpcChatResponse chat(@RequestBody NpcChatRequest request) {
        return npcService.chat(request);
    }
}
