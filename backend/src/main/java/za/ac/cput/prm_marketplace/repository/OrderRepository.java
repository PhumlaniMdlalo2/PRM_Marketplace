package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByBuyerId(UUID buyerId);

    List<Order> findByBuyerIdAndStatus(UUID buyerId, OrderStatus status);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    Optional<Order> findByIdAndBuyerId(UUID id, UUID buyerId);

    boolean existsByIdAndBuyerId(UUID id, UUID buyerId);

    long countByBuyerId(UUID buyerId);

    long countByBuyerIdAndStatus(UUID buyerId, OrderStatus status);
}