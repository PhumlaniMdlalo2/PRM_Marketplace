package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.AddressRepository;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements IOrderService {

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED);

    /** Only these roles may move an order through its lifecycle on the seller's side. */
    private static final Set<Role> MAY_ADVANCE_ORDER = EnumSet.of(Role.VENDOR, Role.FACULTY);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            CartItemRepository cartItemRepository,
                            AddressRepository addressRepository,
                            OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Order read(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return null;
        }
        return orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
    }

    @Override
    @Transactional
    public Order update(Order order, UUID requesterId) {
        if (order == null || order.getId() == null || requesterId == null) {
            return null;
        }
        Order existing = orderRepository.findByIdAndBuyerId(order.getId(), requesterId).orElse(null);
        if (existing == null) {
            return null;
        }
        if (existing.getStatus() != OrderStatus.PENDING) {
            // Once an order has progressed the buyer's edits would contradict what the seller
            // has already acted on.
            return null;
        }

        // Only the delivery address is buyer-editable. Status, total, buyer and timestamps stay
        // as they are, and the line items are fixed once the order exists.
        Address requested = order.getShippingAddress();
        if (requested == null) {
            existing.setShippingAddress(null);
        } else {
            // Ownership is checked, but the *stored* row is what gets attached. Attaching the
            // deserialized copy instead would let the caller keep the same address id and quietly
            // rewrite its street and city through an order update, bypassing address validation
            // and leaving the caller's address book disagreeing with their delivered orders.
            Address owned = findOwnedBy(requesterId, requested.getId());
            if (owned == null) {
                // Naming somebody else's address, or one that does not exist, must not quietly
                // redirect the delivery.
                return null;
            }
            existing.setShippingAddress(owned);
        }
        return orderRepository.save(existing);
    }

    /** The persisted address belonging to the caller, or null when the id is not theirs. */
    private Address findOwnedBy(UUID requesterId, UUID addressId) {
        if (addressId == null) {
            return null;
        }
        return addressRepository.findById(addressId)
                .filter(stored -> stored.getUser() != null)
                .filter(stored -> requesterId.equals(stored.getUser().getId()))
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return false;
        }
        Order existing = orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
        if (existing == null) {
            return false;
        }
        // Cancelling is the reversible path: it returns the reserved stock. A hard delete of an
        // order that has already reserved stock would leak that stock permanently.
        return cancelInternal(existing);
    }

    @Override
    public List<Order> getAll(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(requesterId);
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
    public Order checkout(UUID buyerId, UUID shippingAddressId) {
        if (buyerId == null) {
            return null;
        }
        User buyer = userRepository.findById(buyerId).orElse(null);
        if (buyer == null) {
            return null;
        }

        // The cart comes from the database, so a caller cannot order a product that is not in it
        // and cannot pick a quantity the server never agreed to.
        List<CartItem> cartItems = cartItemRepository.findByUser_Id(buyerId);
        if (cartItems.isEmpty()) {
            return null;
        }

        Address shippingAddress = null;
        if (shippingAddressId != null) {
            shippingAddress = addressRepository.findById(shippingAddressId).orElse(null);
            if (shippingAddress == null
                    || shippingAddress.getUser() == null
                    || !buyerId.equals(shippingAddress.getUser().getId())) {
                // Somebody else's address, or one that does not exist. Either way the caller does
                // not get to have it shipped to them.
                return null;
            }
        }

        Order order = new Order.Builder()
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setShippingAddress(shippingAddress)
                .build();

        List<CartItem> purchased = new ArrayList<>();

        // Price and reserve in one pass, before anything is written. Reserving first and pricing
        // from the reserved row is safe because decrementStock refuses to go below zero.
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null || cartItem.getQuantity() <= 0) {
                continue;
            }
            if (productRepository.decrementStock(product.getId(), cartItem.getQuantity()) == 0) {
                // Either the product was deactivated or somebody else took the last units first.
                // The transaction rolls back, releasing the stock reserved by earlier lines.
                return null;
            }
            Product reserved = productRepository.findById(product.getId()).orElse(null);
            if (reserved == null) {
                return null;
            }
            order.addItem(new OrderItem.Builder()
                    .setOrder(order)
                    .setProduct(reserved)
                    .setQuantity(cartItem.getQuantity())
                    .setPriceAtPurchase(reserved.getPrice())
                    .build());
            purchased.add(cartItem);
        }

        if (order.getItems().isEmpty()) {
            return null;
        }
        order.setTotalAmount(totalOf(order));

        Order saved = orderRepository.save(order);

        // Emptying the cart is scoped to the lines that actually made it into the order.
        cartItemRepository.deleteAll(purchased);
        return saved;
    }

    @Override
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status, UUID requesterId, Role role) {
        if (id == null || status == null || requesterId == null || !MAY_ADVANCE_ORDER.contains(role)) {
            return null;
        }
        Order existing = orderRepository.findById(id).orElse(null);
        if (existing == null || existing.getStatus() == status) {
            return null;
        }
        if (!mayActOn(existing, requesterId, role)) {
            return null;
        }
        if (!isLegalTransition(existing.getStatus(), status)) {
            return null;
        }
        existing.setStatus(status);
        return orderRepository.save(existing);
    }

    /**
     * Faculty supervise every order. A vendor only speaks for the products they sell, so they are
     * admitted only when one of the order's line items is theirs.
     */
    private boolean mayActOn(Order order, UUID requesterId, Role role) {
        if (role == Role.FACULTY) {
            return true;
        }
        return orderItemRepository.existsByOrderIdAndVendorUserId(order.getId(), requesterId);
    }

    /**
     * The happy path runs forwards one step at a time, and separately allows cancellation and
     * refunding while an order is still in flight.
     *
     * <p>Being explicit about the permitted edges is the point: without this, any status could be
     * set to any other, so a cancelled order could be marked delivered and a delivered one could
     * be reopened. Terminal states have no outgoing edge, which is what makes that impossible.
     */
    private static boolean isLegalTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.SHIPPED
                    || to == OrderStatus.CANCELLED
                    || to == OrderStatus.REFUNDED;
            case SHIPPED -> to == OrderStatus.DELIVERED || to == OrderStatus.REFUNDED;
            case DELIVERED, CANCELLED, REFUNDED -> false;
        };
    }

    @Override
    @Transactional
    public boolean cancel(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return false;
        }
        Order existing = orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
        if (existing == null) {
            return false;
        }
        return cancelInternal(existing);
    }

    /**
     * Marks a cancellable order cancelled and hands its stock back. Restocking happens before the
     * status change is written, and both live in the caller's transaction, so a failure part way
     * through leaves neither applied.
     */
    private boolean cancelInternal(Order existing) {
        if (!CANCELLABLE.contains(existing.getStatus())) {
            return false;
        }
        List<OrderItem> items = existing.getItems();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() != null) {
                    productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
                }
            }
        }
        existing.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(existing);
        return true;
    }

    /**
     * Sums the order's own lines, using the prices captured at purchase. Reading them back off the
     * order rather than the cart means the total cannot drift from what the line items say.
     */
private BigDecimal totalOf(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getPriceAtPurchase() == null) {
                continue;
            }
            total = total.add(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
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