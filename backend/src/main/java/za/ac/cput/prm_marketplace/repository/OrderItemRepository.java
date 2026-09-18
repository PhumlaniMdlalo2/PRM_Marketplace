package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.prm_marketplace.domain.OrderItem;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByProductId(UUID productId);
}