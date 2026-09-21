package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;

import java.util.List;
import java.util.UUID;

@Service
public class OrderItemServiceImpl implements IOrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public OrderItem create(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        return orderItemRepository.save(orderItem);
    }

    @Override
    public OrderItem read(UUID id) {
        if (id == null) {
            return null;
        }
        return orderItemRepository.findById(id).orElse(null);
    }

    @Override
    public OrderItem update(OrderItem orderItem) {
        if (orderItem == null || orderItem.getId() == null ||
                !orderItemRepository.existsById(orderItem.getId())) {
            return null;
        }
        return orderItemRepository.save(orderItem);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !orderItemRepository.existsById(id)) {
            return false;
        }

        orderItemRepository.deleteById(id);
        return true;
    }

    @Override
    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }
}