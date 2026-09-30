package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderItemServiceImpl service;

    private UUID id;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        service = new OrderItemServiceImpl(orderItemRepository);
        id = UUID.randomUUID();
        orderItem = new OrderItem.Builder()
                .setId(id)
                .setProduct(new Product.Builder().id(UUID.randomUUID()).name("Textbook").build())
                .setQuantity(2)
                .setPriceAtPurchase(new java.math.BigDecimal("45.00"))
                .build();
    }

    @Test
    @DisplayName("create returns null for a null order item")
    void create_nullReturnsNull() {
        assertThat(service.create(null)).isNull();

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("create delegates to the repository")
    void create_savesOrderItem() {
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);

        assertThat(service.create(orderItem)).isSameAs(orderItem);

        verify(orderItemRepository).save(orderItem);
    }

    @Test
    @DisplayName("read returns null for a null id")
    void read_nullIdReturnsNull() {
        assertThat(service.read(null)).isNull();

        verify(orderItemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("read returns the stored order item")
    void read_returnsOrderItem() {
        when(orderItemRepository.findById(id)).thenReturn(Optional.of(orderItem));

        assertThat(service.read(id)).isSameAs(orderItem);
    }

    @Test
    @DisplayName("read returns null when the order item is absent")
    void read_missingReturnsNull() {
        when(orderItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.read(id)).isNull();
    }

    @Test
    @DisplayName("update returns null for a null order item")
    void update_nullReturnsNull() {
        assertThat(service.update(null)).isNull();

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null when the order item has no id")
    void update_nullIdReturnsNull() {
        OrderItem noId = new OrderItem.Builder()
                .setProduct(new Product.Builder().id(UUID.randomUUID()).name("Textbook").build())
                .setQuantity(1)
                .build();

        assertThat(service.update(noId)).isNull();

        verify(orderItemRepository, never()).existsById(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null when the order item does not exist")
    void update_missingReturnsNull() {
        when(orderItemRepository.existsById(id)).thenReturn(false);

        assertThat(service.update(orderItem)).isNull();

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("update saves an existing order item")
    void update_savesOrderItem() {
        when(orderItemRepository.existsById(id)).thenReturn(true);
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);

        assertThat(service.update(orderItem)).isSameAs(orderItem);

        verify(orderItemRepository).save(orderItem);
    }

    @Test
    @DisplayName("delete returns false for a null id")
    void delete_nullIdReturnsFalse() {
        assertThat(service.delete(null)).isFalse();

        verify(orderItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false when the order item does not exist")
    void delete_missingReturnsFalse() {
        when(orderItemRepository.existsById(id)).thenReturn(false);

        assertThat(service.delete(id)).isFalse();

        verify(orderItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete removes an existing order item")
    void delete_removesOrderItem() {
        when(orderItemRepository.existsById(id)).thenReturn(true);

        assertThat(service.delete(id)).isTrue();

        verify(orderItemRepository).deleteById(id);
    }

    @Test
    @DisplayName("getAll returns every order item")
    void getAll_returnsList() {
        when(orderItemRepository.findAll()).thenReturn(List.of(orderItem));

        assertThat(service.getAll()).containsExactly(orderItem);
    }
}
