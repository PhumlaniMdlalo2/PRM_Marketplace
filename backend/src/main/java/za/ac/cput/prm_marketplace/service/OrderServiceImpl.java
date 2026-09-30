package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements IOrderService {

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order create(Order order) {
        if (order == null) {
            return null;
        }
        return orderRepository.save(order);
    }

    @Override
    public Order read(UUID id) {
        if (id == null) {
            return null;
        }
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    public Order update(Order order) {
        if (order == null || order.getId() == null || !orderRepository.existsById(order.getId())) {
            return null;
        }
        return orderRepository.save(order);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !orderRepository.existsById(id)) {
            return false;
        }
        orderRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public List<Order> getByBuyer(UUID buyerId) {
        if (buyerId == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    public List<Order> getByBuyerAndStatus(UUID buyerId, OrderStatus status) {
        if (buyerId == null || status == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdAndStatus(buyerId, status);
    }

    @Override
    @Transactional
    public Order checkout(User buyer, List<CartItem> cartItems, Address shippingAddress) {
        if (buyer == null) {
            return null;
        }
        if (cartItems == null || cartItems.isEmpty()) {
            return null;
        }

        Order order = new Order.Builder()
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setTotalAmount(calculateTotal(cartItems))
                .setShippingAddress(shippingAddress)
                .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null) {
                continue;
            }
            OrderItem orderItem = new OrderItem.Builder()
                    .setOrder(order)
                    .setProduct(product)
                    .setQuantity(cartItem.getQuantity())
                    .setPriceAtPurchase(product.getPrice())
                    .build();
            order.addItem(orderItem);
        }

        if (order.getItems().isEmpty()) {
            return null;
        }

        return orderRepository.save(order);
    }

    @Override
    public Order updateStatus(UUID id, OrderStatus status) {
        if (id == null || status == null) {
            return null;
        }
        Order existing = orderRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setStatus(status);
        return orderRepository.save(existing);
    }

    @Override
    public boolean cancel(UUID id, UUID buyerId) {
        if (id == null || buyerId == null) {
            return false;
        }
        Order existing = orderRepository.findByIdAndBuyerId(id, buyerId).orElse(null);
        if (existing == null || !CANCELLABLE.contains(existing.getStatus())) {
            return false;
        }
        existing.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(existing);
        return true;
    }

    @Override
    public BigDecimal calculateTotal(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            if (cartItem == null || cartItem.getProduct() == null) {
                continue;
            }
            BigDecimal price = cartItem.getProduct().getPrice();
            if (price == null) {
                continue;
            }
            total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        return total;
    }
}