package za.ac.cput.prm_marketplace.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentDiscussionGroupResponse(
        UUID id,
        String name,
        String description,
        String campus,
        long memberCount,
        boolean joined,
        LocalDateTime createdAt
) {
}
