package za.ac.cput.prm_marketplace.mapper;

import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.dto.UserResponse;

import java.util.ArrayList;
import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCampus(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }

    public static List<UserResponse> toResponseList(List<User> users) {
        List<UserResponse> responses = new ArrayList<>();
        if (users == null) {
            return responses;
        }
        for (User user : users) {
            responses.add(toResponse(user));
        }
        return responses;
    }
}