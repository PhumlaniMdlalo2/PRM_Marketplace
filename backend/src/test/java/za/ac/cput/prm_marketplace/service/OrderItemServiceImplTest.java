package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private UUID id;
    private UUID orderId;
    private UUID buyerId;
    private UUID intruderId;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
    }

    private OrderItem sampleItem() {
        return new OrderItem.Builder()
                .setId(id)
                .setQuantity(2)
                .setPriceAtPurchase(new BigDecimal("125.50"))
                .build();
    }

    @Test
    @DisplayName("reading a line item is scoped to the buyer of its order")
    void read_isScopedToTheBuyer() {
        OrderItem item = sampleItem();
        when(orderItemRepository.findByIdAndOrderBuyerId(id, buyerId)).thenReturn(Optional.of(item));

        assertThat(orderItemService.read(id, buyerId)).isSameAs(item);
    }

    @Test
    @DisplayName("a line item on somebody else's order is not found")
    void read_ofSomebodyElsesItemIsNull() {
        when(orderItemRepository.findByIdAndOrderBuyerId(id, intruderId)).thenReturn(Optional.empty());

        assertThat(orderItemService.read(id, intruderId)).isNull();
    }

    @Test
    @DisplayName("read is null-safe")
    void read_isNullSafe() {
        assertThat(orderItemService.read(null, buyerId)).isNull();
        assertThat(orderItemService.read(id, null)).isNull();

        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("the lines on a caller's order come back oldest first")
    void getByOrderId_returnsTheCallersLines() {
        List<OrderItem> items = List.of(sampleItem());
        when(orderItemRepository.findByOrderIdAndOrderBuyerIdOrderByCreatedAtAsc(orderId, buyerId))
                .thenReturn(items);

        assertThat(orderItemService.getByOrderId(orderId, buyerId)).isSameAs(items);
    }

    @Test
    @DisplayName("another account's order yields no lines")
    void getByOrderId_isEmptyForAnOrderTheCallerDoesNotOwn() {
        when(orderItemRepository.findByOrderIdAndOrderBuyerIdOrderByCreatedAtAsc(orderId, intruderId))
                .thenReturn(List.of());

        assertThat(orderItemService.getByOrderId(orderId, intruderId)).isEmpty();
    }

    @Test
    @DisplayName("listing is empty for null inputs")
    void getByOrderId_isEmptyForNulls() {
        assertThat(orderItemService.getByOrderId(null, buyerId)).isEmpty();
        assertThat(orderItemService.getByOrderId(orderId, null)).isEmpty();
    }

    @Test
    @DisplayName("there is no way to write a line item through this service")
    void theServiceExposesNoWrites() {
        // Guards the shape of the contract, not just the behaviour: the write methods were removed
        // rather than guarded, so a caller cannot set a price, quantity or order.
        assertThat(java.util.Arrays.stream(IOrderItemService.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("read", "getByOrderId");
    }

    @Test
    @DisplayName("the unguarded finders are not used by the API path")
    void theUnguardedFindersAreNotUsed() {
        orderItemService.read(id, buyerId);
        orderItemService.getByOrderId(orderId, buyerId);

        verify(orderItemRepository).findByIdAndOrderBuyerId(any(), any());
        verify(orderItemRepository).findByOrderIdAndOrderBuyerIdOrderByCreatedAtAsc(any(), any());
    }
}