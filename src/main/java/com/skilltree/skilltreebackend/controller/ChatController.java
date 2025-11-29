package com.skilltree.skilltreebackend.controller;

import com.skilltree.skilltreebackend.dto.ChatRequest;
import com.skilltree.skilltreebackend.dto.ChatResponse;
import com.skilltree.skilltreebackend.service.ChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    private final ChatService fastAI;
    private final ChatService deepAI;

    public ChatController(
            @Qualifier("fast") ChatService fastAI,
            @Qualifier("deep") ChatService deepAI
    ) {
        this.fastAI = fastAI;
        this.deepAI = deepAI;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {

        String node = req.getNode();
        String message = req.getMessage();
        String mode = req.getMode() == null ? "fast" : req.getMode();

        String reply;

        if (mode.equals("deep")) {
            reply = deepAI.chat(node, message);
        } else {
            reply = fastAI.chat(node, message);
        }

        return new ChatResponse(reply);
    }
}
