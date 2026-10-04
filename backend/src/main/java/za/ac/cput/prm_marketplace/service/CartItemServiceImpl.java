package za.ac.cput.prm_marketplace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.factory.CartItemFactory;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartItemServiceImpl implements ICartItemService {

    /** Guards against a request asking for an implausible quantity of a single product. */
    private static final int MAX_QUANTITY_PER_LINE = 99;

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CartItemServiceImpl(CartItemRepository cartItemRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CartItem addToCart(UUID requesterId, UUID productId, int quantity) {
        if (requesterId == null || productId == null) {
            throw new IllegalArgumentException("A product is required");
        }
        requireSaneQuantity(quantity);

        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requesterId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not available: " + productId);
        }
        // A seller buying their own listing is a purchase that cannot mean anything: the seller is on
        // both sides of it, so the seller-side order status they are supposed to advance is theirs to
        // approve, and the money goes nowhere. The product page already disables the button, but the
        // client is not the boundary — this has to hold for any caller.
        if (product.getVendor() != null
                && product.getVendor().getUser() != null
                && product.getVendor().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot buy your own listing: " + productId);
        }

        Optional<CartItem> existing = cartItemRepository.findByUser_IdAndProduct_Id(requesterId, productId);
        if (existing.isPresent()) {
            CartItem current = existing.get();
            CartItem merged = new CartItem.Builder()
                    .copy(current)
                    .quantity(clamp(current.getQuantity() + quantity))
                    .build();
            return cartItemRepository.save(merged);
        }

        return cartItemRepository.save(CartItemFactory.createCartItem(user, product, quantity));
    }

    @Override
    public CartItem read(UUID id, UUID requesterId) {
        CartItem item = find(id);
        return isOwnedBy(item, requesterId) ? item : null;
    }

    @Override
    @Transactional
    public CartItem updateQuantity(UUID id, UUID requesterId, int quantity) {
        CartItem existing = read(id, requesterId);
        if (existing == null) {
            return null;
        }
        if (quantity <= 0) {
            cartItemRepository.deleteById(id);
            return null;
        }
        CartItem updated = new CartItem.Builder()
                .copy(existing)
                .quantity(clamp(quantity))
                .build();
        return cartItemRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (read(id, requesterId) == null) {
            return false;
        }
        cartItemRepository.deleteById(id);
        return true;
    }

    @Override
    public List<CartItem> getByUser(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return cartItemRepository.findByUser_Id(requesterId);
    }

    @Override
    @Transactional
    public void clearCart(UUID requesterId) {
        if (requesterId != null) {
            cartItemRepository.deleteByUser_Id(requesterId);
        }
    }

    private void requireSaneQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (quantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException("Quantity must not exceed " + MAX_QUANTITY_PER_LINE);
        }
    }

    private int clamp(int quantity) {
        return Math.min(quantity, MAX_QUANTITY_PER_LINE);
    }

    private CartItem find(UUID id) {
        if (id == null) {
            return null;
        }
        return cartItemRepository.findById(id).orElse(null);
    }

    private boolean isOwnedBy(CartItem item, UUID requesterId) {
        return item != null
                && item.getUser() != null
                && item.getUser().getId() != null
                && item.getUser().getId().equals(requesterId);
    }
}