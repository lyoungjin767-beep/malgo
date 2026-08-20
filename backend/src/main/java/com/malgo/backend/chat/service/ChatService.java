package com.malgo.backend.chat.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.malgo.backend.customization.entity.SpeechStyle;
import com.malgo.backend.customization.entity.UserCustomization;
import com.malgo.backend.customization.repository.UserCustomizationRepository;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final OpenAIClient client;
    private final MemberRepository memberRepository;
    private final UserCustomizationRepository userCustomizationRepository;

    public ChatService(
            @Value("${openai.api-key}") String apiKey,
            MemberRepository memberRepository,
            UserCustomizationRepository userCustomizationRepository
    ) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.memberRepository = memberRepository;
        this.userCustomizationRepository = userCustomizationRepository;
    }

    public String chat(String username, String message) {

        String speechStyle = findSelectedSpeechStyle(username);

        ResponseCreateParams params =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .input(buildPrompt(message, speechStyle))
                        .build();

        Response response =
                client.responses().create(params);

        StringBuilder answer = new StringBuilder();

        for (var item : response.output()) {
            if (item.message().isEmpty()) {
                continue;
            }

            for (var content : item.message().get().content()) {
                content.outputText()
                        .ifPresent(outputText -> answer.append(outputText.text()));
            }
        }

        return answer.toString();
    }

    private String findSelectedSpeechStyle(String username) {

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "로그인한 회원 정보를 찾을 수 없습니다."
                ));

        return userCustomizationRepository.findByMemberId(member.getId())
                .map(this::getOnlySpeechStyle)
                .map(SpeechStyle::name)
                .orElse(SpeechStyle.PLAIN.name());
    }

    private SpeechStyle getOnlySpeechStyle(UserCustomization customization) {

        if (customization.getSpeechStyles().size() != 1) {
            throw new IllegalStateException(
                    "AI 답변 말투는 하나만 선택해야 합니다."
            );
        }

        return customization.getSpeechStyles().iterator().next();
    }

    private String buildPrompt(String message, String speechStyle) {
        return """
                너는 Malgo의 AI 글로벌 커뮤니케이션 도우미다.

                선택된 말투: %s

                선택된 말투는 답변의 실제 어투를 정한다. 말투를 설명하거나 이름을 언급하지 말고,
                제목을 제외한 답변의 모든 한국어 자연어 문장에 처음부터 끝까지 일관되게 적용하라.

                - FORMAL: 공식적이고 전문적인 격식체를 사용한다.
                - POLITE: 예의 있고 부드러운 정중체를 사용한다.
                - FRIENDLY: 친근하고 편안한 대화체를 사용한다.
                - WARM: 따뜻하고 배려가 느껴지는 다정체를 사용한다.
                - PLAYFUL: 무례하지 않은 가볍고 유쾌한 장난체를 사용한다.
                - PLAIN: 과장 없이 간결하고 자연스러운 담백체를 사용한다.
                - SINCERE: 진솔하고 진정성 있는 진성체를 사용한다.
                - EMOTIONAL: 감정과 분위기가 자연스럽게 느껴지는 감성체를 사용한다.
                - DIALECT: 한국어 설명을 포함한 모든 한국어 자연어 문장을 실제 한국어 사투리 어조로
                  작성한다. 특정 지역이 지정되지 않은 경우에도 이해하기 쉬운 자연스러운 사투리를
                  일관되게 사용하며, 표준어로 바꾸지 않는다.

                사용자 메시지:
                %s
                """.formatted(speechStyle, message);
    }
}
