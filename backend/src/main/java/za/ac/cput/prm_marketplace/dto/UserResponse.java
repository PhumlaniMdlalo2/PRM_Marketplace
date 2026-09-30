package za.ac.cput.prm_marketplace.dto;

import za.ac.cput.prm_marketplace.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        String phone,
        String avatarUrl,
        boolean verified,
        LocalDateTime createdAt
) {
}