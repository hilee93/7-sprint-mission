package com.sprint.mission.discodeit.dto.response.binarycontent;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;

import java.util.UUID;

public record BinaryContentEventDto(
        UUID id,
        BinaryContentStatus status
) {
}
