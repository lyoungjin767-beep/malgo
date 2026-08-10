package com.malgo.backend.service;

import com.malgo.backend.dto.ConversationCreateRequest;
import com.malgo.backend.dto.ConversationMessageRequest;
import com.malgo.backend.dto.ConversationMessageResponse;
import com.malgo.backend.dto.ConversationResponse;
import com.malgo.backend.entity.AiPartner;
import com.malgo.backend.entity.Conversation;
import com.malgo.backend.entity.ConversationMessage;
import com.malgo.backend.repository.AiPartnerRepository;
import com.malgo.backend.repository.ConversationMessageRepository;
import com.malgo.backend.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malgo.backend.ai.OpenAiClient;
import com.malgo.backend.dto.ConversationChatResponse;

import java.util.List;

// 대화방 생성과 메시지 저장/조회를 처리

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AiPartnerRepository aiPartnerRepository;
    private final OpenAiClient openAiClient;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            AiPartnerRepository aiPartnerRepository,
            OpenAiClient openAiClient
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiPartnerRepository = aiPartnerRepository;
        this.openAiClient = openAiClient;
    }

    // 선택한 AI 상대를 기준으로 새 대화방을 생성
    @Transactional
    public ConversationResponse createConversation(
            ConversationCreateRequest request
    ) {
        AiPartner partner = aiPartnerRepository.findById(request.aiPartnerId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + request.aiPartnerId()
                        )
                );

        Conversation conversation = new Conversation(
                partner,
                request.situation()
        );

        Conversation saved =
                conversationRepository.save(conversation);

        return new ConversationResponse(
                saved.getId(),
                partner.getId(),
                partner.getName(),
                saved.getSituation()
        );
    }

    // 사용자의 메시지를 저장하고 AI 상대 정보를 이용해 OpenAI 응답을 생성한 뒤 AI 응답까지 자동으로 저장
    @Transactional
    public ConversationChatResponse sendMessage(
            Long conversationId,
            ConversationMessageRequest request
    ) {

        // 1. 대화방 조회
        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "대화방을 찾을 수 없습니다. id=" + conversationId
                                )
                        );

        AiPartner partner = conversation.getAiPartner();

        // 2. 사용자 메시지 저장
        ConversationMessage userMessage =
                new ConversationMessage(
                        conversation,
                        "USER",
                        request.content()
                );

        ConversationMessage savedUserMessage =
                messageRepository.save(userMessage);

        // 3. 선택된 AI 상대의 정보를 이용해 실제 OpenAI 응답 생성
        String aiContent = openAiClient.chat(
                partner.getName(),
                partner.getTargetCountry(),
                partner.getRelationshipType(),
                partner.getSpeechStyle(),
                partner.getCharacteristic(),
                conversation.getSituation(),
                request.content()
        );

        // 4. 생성된 AI 응답 자동 저장
        ConversationMessage assistantMessage =
                new ConversationMessage(
                        conversation,
                        "ASSISTANT",
                        aiContent
                );

        ConversationMessage savedAssistantMessage =
                messageRepository.save(assistantMessage);

        // 5. USER + ASSISTANT 메시지를 함께 프론트에 반환
        return new ConversationChatResponse(
                new ConversationMessageResponse(
                        savedUserMessage.getId(),
                        savedUserMessage.getSenderType(),
                        savedUserMessage.getContent(),
                        savedUserMessage.getCreatedAt()
                ),
                new ConversationMessageResponse(
                        savedAssistantMessage.getId(),
                        savedAssistantMessage.getSenderType(),
                        savedAssistantMessage.getContent(),
                        savedAssistantMessage.getCreatedAt()
                )
        );
    }

    // 특정 대화방의 메시지를 오래된 순서부터 조회
    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> getMessages(
            Long conversationId
    ) {
        // 존재하지 않는 대화방인지 먼저 확인
        if (!conversationRepository.existsById(conversationId)) {
            throw new IllegalArgumentException(
                    "대화방을 찾을 수 없습니다. id=" + conversationId
            );
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> new ConversationMessageResponse(
                        message.getId(),
                        message.getSenderType(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
    }
}