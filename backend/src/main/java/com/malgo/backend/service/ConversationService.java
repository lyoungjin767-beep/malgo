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
import com.malgo.backend.dto.ConversationSummaryResponse;
import com.malgo.backend.entity.ConversationSummary;
import com.malgo.backend.repository.ConversationSummaryRepository;
import com.malgo.backend.dto.ConversationListResponse;
import com.malgo.backend.dto.ConversationStatisticsResponse;
import com.malgo.backend.dto.ConversationDetailResponse;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

// 대화방 생성과 메시지 저장/조회를 처리

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AiPartnerRepository aiPartnerRepository;
    private final OpenAiClient openAiClient;
    private final ConversationSummaryRepository summaryRepository;
    private final MemberRepository memberRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            AiPartnerRepository aiPartnerRepository,
            OpenAiClient openAiClient,
            ConversationSummaryRepository summaryRepository,
            MemberRepository memberRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiPartnerRepository = aiPartnerRepository;
        this.openAiClient = openAiClient;
        this.summaryRepository = summaryRepository;
        this.memberRepository = memberRepository;
    }

    // 선택한 AI 상대를 기준으로 새 대화방을 생성
    @Transactional
    public ConversationResponse createConversation(
            ConversationCreateRequest request
    ) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다. id=" + request.memberId()
                        )
                );

        AiPartner partner = aiPartnerRepository.findById(request.aiPartnerId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + request.aiPartnerId()
                        )
                );

        Conversation conversation = new Conversation(
                member,
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
            Long memberId,
            Long conversationId,
            ConversationMessageRequest request
    ) {

        // 1. 대화방 조회
        Conversation conversation =
                getOwnedConversation(memberId, conversationId);

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

        // 새로운 메시지가 들어왔으므로 대화방의 최근 활동 시간 갱신
        conversation.updateLastActivity();

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
            Long memberId,
            Long conversationId
    ) {
        // 해당 회원의 대화방인지 확인
        getOwnedConversation(memberId, conversationId);

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

    // 특정 대화방의 전체 메시지를 가져와 OpenAI를 이용해 대화 내용을 요약
    @Transactional
    public ConversationSummaryResponse summarizeConversation(
            Long memberId,
            Long conversationId
    ) {

        // 해당 회원의 대화방인지 확인
        Conversation conversation =
                getOwnedConversation(memberId, conversationId);

        // 2. 해당 대화방의 메시지를 시간순으로 조회
        List<ConversationMessage> messages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(
                        conversation.getId()
                );

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "요약할 대화 내용이 없습니다."
            );
        }

        // 3. OpenAI에 전달할 대화 텍스트 생성
        String conversationText = messages.stream()
                .map(message ->
                        message.getSenderType()
                                + ": "
                                + message.getContent()
                )
                .collect(Collectors.joining("\n"));

        // 4. OpenAI로 대화 요약 생성
        String summary =
                openAiClient.summarizeConversation(conversationText);

        // 5. 생성된 요약을 DB에 저장
        ConversationSummary conversationSummary =
                new ConversationSummary(
                        conversation,
                        summary
                );

        ConversationSummary savedSummary =
                summaryRepository.save(conversationSummary);

        // 6. 저장된 요약 결과를 프론트에 반환
        return new ConversationSummaryResponse(
                savedSummary.getId(),
                conversationId,
                savedSummary.getSummary(),
                savedSummary.getCreatedAt()
        );
    }

    // 특정 대화방에 저장된 요약 기록을 최신순으로 조회
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversationSummaries(
            Long memberId,
            Long conversationId
    ) {

        // 해당 회원의 대화방인지 확인
        getOwnedConversation(memberId, conversationId);

        return summaryRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId)
                .stream()
                .map(summary -> new ConversationSummaryResponse(
                        summary.getId(),
                        conversationId,
                        summary.getSummary(),
                        summary.getCreatedAt()
                ))
                .toList();
    }

    // 대화방과 연결된 메시지/요약을 함께 삭제
    @Transactional
    public void deleteConversation(Long memberId, Long conversationId) {

        Conversation conversation =
                getOwnedConversation(memberId, conversationId);

        // FK 제약 때문에 자식 데이터부터 삭제
        messageRepository.deleteByConversationId(conversationId);
        summaryRepository.deleteByConversationId(conversationId);

        // 마지막으로 대화방 삭제
        conversationRepository.delete(conversation);
    }

    // 저장된 대화방을 situation 기준으로 집계
    // 예: BUSINESS -> 3, DAILY -> 2
    //전체 대화 수를 기준으로 비율도 함께 계산
    @Transactional(readOnly = true)
    public ConversationStatisticsResponse getConversationStatistics(Long memberId) {

        List<Conversation> conversations =
                conversationRepository
                        .findByMemberIdOrderByUpdatedAtDesc(memberId);

        long totalCount = conversations.size();

        Map<String, Long> counts = conversations.stream()
                .filter(conversation ->
                        conversation.getSituation() != null
                                && !conversation.getSituation().isBlank()
                )
                .collect(
                        Collectors.groupingBy(
                                Conversation::getSituation,
                                LinkedHashMap::new,
                                Collectors.counting()
                        )
                );

        Map<String, Double> percentages =
                new LinkedHashMap<>();

        counts.forEach((situation, count) -> {

            double percentage =
                    totalCount == 0
                            ? 0.0
                            : ((double) count / totalCount) * 100;

            // 소수점 첫째 자리까지
            percentage =
                    Math.round(percentage * 10.0) / 10.0;

            percentages.put(
                    situation,
                    percentage
            );
        });

        return new ConversationStatisticsResponse(
                totalCount,
                counts,
                percentages
        );
    }

    // 특정 대화방의 상세 정보를 조회
    // AI 상대 정보와 저장된 메시지를 함께 반환
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(
            Long memberId,
            Long conversationId
    ) {

        Conversation conversation =
                getOwnedConversation(memberId, conversationId);

        // 2. 연결된 AI 상대 정보
        AiPartner partner = conversation.getAiPartner();

        // 3. 대화방 메시지를 시간순으로 조회
        List<ConversationMessageResponse> messages =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(conversationId)
                        .stream()
                        .map(message ->
                                new ConversationMessageResponse(
                                        message.getId(),
                                        message.getSenderType(),
                                        message.getContent(),
                                        message.getCreatedAt()
                                )
                        )
                        .toList();

        // 4. 상세 정보 반환
        return new ConversationDetailResponse(
                conversation.getId(),
                partner.getId(),
                partner.getName(),
                partner.getTargetCountry(),
                partner.getRelationshipType(),
                conversation.getSituation(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    // 특정 대화방의 가장 최근 요약 1건을 조회
    @Transactional(readOnly = true)
    public ConversationSummaryResponse getLatestConversationSummary(
            Long memberId,
            Long conversationId
    ) {
        // 해당 회원의 대화방인지 확인
        getOwnedConversation(memberId, conversationId);

        ConversationSummary summary =
                summaryRepository
                        .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "저장된 대화 요약이 없습니다."
                                )
                        );

        return new ConversationSummaryResponse(
                summary.getId(),
                conversationId,
                summary.getSummary(),
                summary.getCreatedAt()
        );
    }

    // 특정 회원의 대화방을 최근 활동 순서로 조회
    @Transactional(readOnly = true)
    public List<ConversationListResponse> getConversationsByMember(
            Long memberId
    ) {

        // 회원 존재 여부 확인
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "회원을 찾을 수 없습니다. id=" + memberId
            );
        }

        return conversationRepository
                .findByMemberIdOrderByUpdatedAtDesc(memberId)
                .stream()
                .map(conversation -> {

                    AiPartner partner = conversation.getAiPartner();

                    // 해당 대화방의 가장 최근 메시지 조회
                    String lastMessage =
                            messageRepository
                                    .findFirstByConversationIdOrderByCreatedAtDesc(
                                            conversation.getId()
                                    )
                                    .map(ConversationMessage::getContent)
                                    .orElse(null);

                    return new ConversationListResponse(
                            conversation.getId(),
                            partner.getId(),
                            partner.getName(),
                            partner.getTargetCountry(),
                            partner.getRelationshipType(),
                            conversation.getSituation(),
                            lastMessage,
                            conversation.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Conversation getOwnedConversation(
            Long memberId,
            Long conversationId
    ) {

        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "대화방을 찾을 수 없습니다. id=" + conversationId
                                )
                        );

        if (!conversation.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "해당 회원의 대화방이 아닙니다."
            );
        }

        return conversation;
    }
}