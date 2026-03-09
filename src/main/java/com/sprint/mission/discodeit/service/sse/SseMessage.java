package com.sprint.mission.discodeit.service.sse;

import java.util.Set;
import java.util.UUID;

public record SseMessage(
        UUID id,
        String eventName,
        Object data,
        Set<UUID> receiverIds
) {
    public boolean isBroadcast() {
        return receiverIds == null || receiverIds.isEmpty();
    }

    public boolean canReceive(UUID receiverId) {
        return isBroadcast() || (receiverId != null && receiverIds.contains(receiverId));
    }
}
