package com.sprint.mission.discodeit.service.sse;

import com.sprint.mission.discodeit.repository.SseEmitterRepository;
import com.sprint.mission.discodeit.repository.SseMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {
    private static final long SSE_TIMEOUT_MILLIS = 1000L * 60 * 60;
    private final SseEmitterRepository sseEmitterRepository;
    private final SseMessageRepository sseMessageRepository;

    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        sseEmitterRepository.save(receiverId, emitter);

        emitter.onCompletion(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onTimeout(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onError(t -> sseEmitterRepository.delete(receiverId, emitter));

        if (!ping(emitter)) {
            sseEmitterRepository.delete(receiverId, emitter);
            return emitter;
        }

        List<SseMessage> lostMessages = sseMessageRepository.findAfter(receiverId, lastEventId);
        for (SseMessage lostMessage : lostMessages) {
            if (!sendToEmitter(emitter, lostMessage)) {
                sseEmitterRepository.delete(receiverId, emitter);
                break;
            }
        }
        return emitter;
    }

    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }

        Set<UUID> targets = receiverIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (targets.isEmpty()) {
            return;
        }

        SseMessage message = new SseMessage(UUID.randomUUID(), eventName, data, targets);
        sseMessageRepository.save(message);

        for (UUID receiverId : targets) {
            List<SseEmitter> emitters = sseEmitterRepository.findAllByReceiverId(receiverId);
            for (SseEmitter emitter : emitters) {
                if (!sendToEmitter(emitter, message)) {
                    sseEmitterRepository.delete(receiverId, emitter);
                }
            }
        }
    }

    public void broadcast(String eventName, Object data) {
        SseMessage message = new SseMessage(UUID.randomUUID(), eventName, data, null);
        sseMessageRepository.save(message);

        Set<UUID> allReceiverIds = sseEmitterRepository.findAllReceiverIds();
        for (UUID receiverId : allReceiverIds) {
            List<SseEmitter> emitters = sseEmitterRepository.findAllByReceiverId(receiverId);
            for (SseEmitter emitter : emitters) {
                if (!sendToEmitter(emitter, message)) {
                    sseEmitterRepository.delete(receiverId, emitter);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1000L * 60 * 30)
    public void cleanUp() {
        Map<UUID, List<SseEmitter>> all = sseEmitterRepository.findAll();
        for (Map.Entry<UUID, List<SseEmitter>> entry : all.entrySet()) {
            UUID receiverId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                if (!ping(emitter)) {
                    sseEmitterRepository.delete(receiverId, emitter);
                }
            }
        }
    }

    private boolean ping(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("keep-alive"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendToEmitter(SseEmitter emitter, SseMessage message) {
        try {
            emitter.send(SseEmitter.event()
                    .id(message.id().toString())
                    .name(message.eventName())
                    .data(message.data()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
