package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderItemServiceImpl implements IOrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItem read(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return null;
        }
        // Ownership is checked through the order's buyer, so a line item belonging to another
        // account reads as missing instead of as somebody else's purchase.
        return orderItemRepository.findByIdAndOrderBuyerId(id, requesterId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getByOrderId(UUID orderId, UUID requesterId) {
        if (orderId == null || requesterId == null) {
            return Collections.emptyList();
        }
        return orderItemRepository.findByOrderIdAndOrderBuyerIdOrderByCreatedAtAsc(orderId, requesterId);
    }
}