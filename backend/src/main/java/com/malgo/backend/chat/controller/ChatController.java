package com.malgo.backend.chat.controller;

import com.malgo.backend.chat.dto.ChatRequest;
import com.malgo.backend.chat.dto.ChatResponse;
import com.malgo.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        String answer = chatService.chat(request.message());

        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
