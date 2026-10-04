package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {

    User read(UUID id);

    /**
     * Applies a profile edit to the account named by {@code userId}.
     *
     * <p>The target is a parameter rather than a field on a submitted entity on purpose. The
     * previous {@code update(User)} read the id off the body and saved the body wholesale, which let
     * a caller name any account and any field on it. Here only the three profile fields move, and
     * the caller is expected to pass its own id.
     *
     * @return the saved account, or null when {@code userId} does not exist
     */
    User updateProfile(UUID userId, String name, String phone, String avatarUrl);

    boolean delete(UUID id);

    List<User> getAll();

    Optional<User> findByEmail(String email);
}