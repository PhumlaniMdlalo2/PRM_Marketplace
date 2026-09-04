package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {

    User create(User user);

    User read(UUID id);

    User update(User user);

    boolean delete(UUID id);

    List<User> getAll();

    Optional<User> findByEmail(String email);
}