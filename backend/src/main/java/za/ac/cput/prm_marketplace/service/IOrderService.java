package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IOrderService {

    Order create(Order order);

    Order read(UUID id);

    Order update(Order order);

    boolean delete(UUID id);

    List<Order> getAll();

    List<Order> getByBuyer(UUID buyerId);

    List<Order> getByBuyerAndStatus(UUID buyerId, OrderStatus status);

    Order checkout(User buyer, List<CartItem> cartItems, Address shippingAddress);

    Order updateStatus(UUID id, OrderStatus status);

    boolean cancel(UUID id, UUID buyerId);

    BigDecimal calculateTotal(List<CartItem> cartItems);
}