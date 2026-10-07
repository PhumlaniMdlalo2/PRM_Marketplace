package za.ac.cput.prm_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import za.ac.cput.prm_marketplace.domain.Role;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        Role role,

        String phone,

        @Size(max = 120, message = "Business name must be at most 120 characters")
        String businessName,

        @Size(max = 120, message = "Registration number must be at most 120 characters")
        String registrationNo
) {
    public RegisterRequest(String name, String email, String password, Role role, String phone) {
        this(name, email, password, role, phone, null, null);
    }
}