package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUser_Id(UUID userId);

    Optional<CartItem> findByUser_IdAndProduct_Id(UUID userId, UUID productId);

    void deleteByUser_Id(UUID userId);

    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);
}
