package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequestDto;
import com.sprint.mission.discodeit.dto.response.message.MessageResponseDto;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {
    private final MessageService messageService;

    @MessageMapping("/messages")
    public MessageResponseDto create(MessageCreateRequestDto messageCreateRequestDto) {
        return messageService.create(messageCreateRequestDto, null);
    }
}
