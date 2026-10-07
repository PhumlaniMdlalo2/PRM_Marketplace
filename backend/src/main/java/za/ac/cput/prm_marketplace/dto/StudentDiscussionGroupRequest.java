package za.ac.cput.prm_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentDiscussionGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description
) {
}
