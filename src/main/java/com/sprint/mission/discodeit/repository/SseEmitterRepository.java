package com.sprint.mission.discodeit.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class SseEmitterRepository {
    private final ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();

    public void save(UUID receiverId, SseEmitter sseEmitter) {
        data.compute(receiverId, (id, emitters) -> {
            List<SseEmitter> target = emitters == null ? new CopyOnWriteArrayList<>() : emitters;
            target.add(sseEmitter);
            return target;
        });
    }

    public List<SseEmitter> findAllByReceiverId(UUID receiverId) {
        List<SseEmitter> emitters = data.get(receiverId);
        if (emitters == null || emitters.isEmpty()) {
            return List.of();
        }
        return List.copyOf(emitters);
    }

    public Set<UUID> findAllReceiverIds() {
        return Set.copyOf(data.keySet());
    }

    public Map<UUID, List<SseEmitter>> findAll() {
        Map<UUID, List<SseEmitter>> result = new HashMap<>();
        data.forEach((receiverId, emitters) ->
                result.put(receiverId, List.copyOf(emitters)));
        return result;
    }

    public void delete(UUID receiverId, SseEmitter sseEmitter) {
        data.computeIfPresent(receiverId, (id, emitters) -> {
            emitters.remove(sseEmitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}


