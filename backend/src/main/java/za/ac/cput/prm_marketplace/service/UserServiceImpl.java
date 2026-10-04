package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User read(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Rebuilds the account through the builder with only the three editable fields replaced.
     *
     * <p>Copying onto the managed entity and rebuilding keeps role, password hash, email,
     * verification state and creation time exactly as they were, because none of them appear as
     * arguments here. That is the point: there is no path by which this method can be made to
     * promote an account or reset a credential, however it is called.
     */
    @Override
    public User updateProfile(UUID userId, String name, String phone, String avatarUrl) {
        User existing = read(userId);
        if (existing == null) {
            return null;
        }
        User updated = new User.Builder()
                .copy(existing)
                .setName(name)
                .setPhone(phone)
                .setAvatarUrl(avatarUrl)
                .build();
        return userRepository.save(updated);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}