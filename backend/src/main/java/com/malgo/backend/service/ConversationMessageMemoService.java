package com.malgo.backend.service;

import com.malgo.backend.entity.ConversationMessage;
import com.malgo.backend.entity.ConversationMessageMemo;
import com.malgo.backend.repository.ConversationMessageMemoRepository;
import com.malgo.backend.repository.ConversationMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationMessageMemoService {

    private final ConversationMessageRepository messageRepository;
    private final ConversationMessageMemoRepository memoRepository;

    public ConversationMessageMemoService(
            ConversationMessageRepository messageRepository,
            ConversationMessageMemoRepository memoRepository
    ) {
        this.messageRepository = messageRepository;
        this.memoRepository = memoRepository;
    }

    @Transactional
    public ConversationMessageMemo saveOrUpdateMemo(
            Long conversationMessageId,
            String content
    ) {
        ConversationMessage message = messageRepository.findById(conversationMessageId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "메시지를 찾을 수 없습니다. id=" + conversationMessageId
                        )
                );

        if (!"ASSISTANT".equals(message.getSenderType())) {
            throw new IllegalArgumentException(
                    "AI 답변에만 메모를 작성할 수 있습니다."
            );
        }

        ConversationMessageMemo memo =
                memoRepository.findByConversationMessageId(conversationMessageId)
                        .orElse(null);

        if (memo == null) {
            memo = new ConversationMessageMemo(
                    message,
                    content
            );
        } else {
            memo.update(content);
        }

        return memoRepository.save(memo);
    }

    @Transactional(readOnly = true)
    public ConversationMessageMemo getMemo(Long conversationMessageId) {
        return memoRepository.findByConversationMessageId(conversationMessageId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "작성된 메모가 없습니다."
                        )
                );
    }

    @Transactional
    public void deleteMemo(Long conversationMessageId) {
        ConversationMessageMemo memo =
                memoRepository.findByConversationMessageId(conversationMessageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "작성된 메모가 없습니다."
                                )
                        );

        memoRepository.delete(memo);
    }
}