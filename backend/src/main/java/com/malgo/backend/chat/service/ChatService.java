package com.malgo.backend.chat.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final OpenAIClient client;

    public ChatService() {
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    public String chat(String message) {

        ResponseCreateParams params =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .input(message)
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
}
