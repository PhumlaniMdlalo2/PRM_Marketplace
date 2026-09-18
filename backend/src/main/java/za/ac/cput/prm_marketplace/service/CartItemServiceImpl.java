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
    public CartItem create(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }

    @Override
    public CartItem read(UUID id) {
        return cartItemRepository.findById(id).orElse(null);
    }

    @Override
    public CartItem update(CartItem cartItem) {
        if (cartItem.getId() != null && cartItemRepository.existsById(cartItem.getId())) {
            return cartItemRepository.save(cartItem);
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        if (cartItemRepository.existsById(id)) {
            cartItemRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<CartItem> getAll() {
        return cartItemRepository.findAll();
    }


    @Override
    public List<CartItem> getByUser(UUID userId) {
        return cartItemRepository.findByUser_Id(userId);
    }

    @Override
    @Transactional
    public CartItem addToCart(UUID userId, UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        Optional<CartItem> existing = cartItemRepository.findByUser_IdAndProduct_Id(userId, productId);

        if (existing.isPresent()) {
            CartItem current = existing.get();
            int newQuantity = current.getQuantity() + quantity;

            CartItem updated = new CartItem.Builder()
                    .copy(current)
                    .quantity(newQuantity)
                    .build();
            return cartItemRepository.save(updated);
        }

        CartItem newItem = CartItemFactory.createCartItem(user, product, quantity);
        return cartItemRepository.save(newItem);
    }

    @Override
    @Transactional
    public CartItem updateQuantity(UUID cartItemId, int quantity) {
        Optional<CartItem> existing = cartItemRepository.findById(cartItemId);
        if (existing.isEmpty()) {
            return null;
        }

        if (quantity <= 0) {
            cartItemRepository.deleteById(cartItemId);
            return null;
        }

        CartItem updated = new CartItem.Builder()
                .copy(existing.get())
                .quantity(quantity)
                .build();
        return cartItemRepository.save(updated);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUser_Id(userId);
    }
}
