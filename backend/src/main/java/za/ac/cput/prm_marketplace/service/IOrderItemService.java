package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.OrderItem;

import java.util.List;
import java.util.UUID;

public interface IOrderItemService {

    OrderItem create(OrderItem orderItem);

    OrderItem read(UUID id);

    OrderItem update(OrderItem orderItem);

    boolean delete(UUID id);

    List<OrderItem> getAll();
}