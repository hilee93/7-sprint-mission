package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.service.sse.SseMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class SseMessageRepository {
    private static final int MAX_STORE_SIZE = 5000;

    private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();

    public void save(SseMessage message) {
        messages.put(message.id(), message);
        eventIdQueue.addLast(message.id());
        trim();
    }

    public List<SseMessage> findAfter(UUID receiverId, UUID lastEventId) {
        if (lastEventId == null) {
            return List.of();
        }

        List<SseMessage> result = new ArrayList<>();
        boolean found = false;

        for (UUID eventId : eventIdQueue) {
            if (!found) {
                if (eventId.equals(lastEventId)) {
                    found = true;
                }
                continue;
            }
            SseMessage message = messages.get(eventId);
            if (message != null && message.canReceive(receiverId)) {
                result.add(message);
            }
        }

        if (!found) {
            for (UUID eventId : eventIdQueue) {
                SseMessage message = messages.get(eventId);
                if (message != null && message.canReceive(receiverId)) {
                    result.add(message);
                }
            }
        }
        return result;
    }

    private void trim() {
        while (eventIdQueue.size() > MAX_STORE_SIZE) {
            UUID oldestId = eventIdQueue.pollFirst();
            if (oldestId != null) {
                messages.remove(oldestId);
            }
        }
    }
}
