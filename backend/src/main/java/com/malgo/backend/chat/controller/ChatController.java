package com.malgo.backend.chat.controller;

import com.malgo.backend.chat.dto.ChatRequest;
import com.malgo.backend.chat.dto.ChatResponse;
import com.malgo.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {

        String answer =
                chatService.chat(authentication.getName(), request.message());

        return ResponseEntity.ok(
                new ChatResponse(answer)
        );
    }
}
