package za.ac.cput.prm_marketplace.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the order lookups against a real database.
 *
 * <p>Every method here takes a buyer id, and several of them are the ownership check behind an order
 * detail page or a cancellation. That makes the scoping itself the thing worth proving: a mocked
 * repository test can only confirm the service passed an id along, not that the predicate actually
 * filtered on the buyer, so "found, but for somebody else" is exactly the failure a mock hides.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;
    private User john;
    private Order pending;
    private Order shipped;
    private Order johNs;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane"));
        john = userRepository.save(buildUser("john"));

        pending = saveOrder(jane, OrderStatus.PENDING, "450.00", LocalDateTime.of(2026, 2, 1, 10, 0));
        shipped = saveOrder(jane, OrderStatus.SHIPPED, "120.50", LocalDateTime.of(2026, 2, 5, 10, 0));
        johNs = saveOrder(john, OrderStatus.PENDING, "999.00", LocalDateTime.of(2026, 2, 6, 10, 0));
    }

    @Test
    @DisplayName("an order list contains only the buyer's own orders")
    void findByBuyerId_excludesOtherBuyers() {
        assertThat(orderRepository.findByBuyerId(jane.getId()))
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(pending.getId(), shipped.getId());
    }

    @Test
    @DisplayName("a buyer with no orders gets an empty list rather than everyone else's")
    void findByBuyerId_isEmptyForSomeoneWhoHasNotOrdered() {
        User carol = userRepository.save(buildUser("carol"));

        assertThat(orderRepository.findByBuyerId(carol.getId())).isEmpty();
    }

    @Test
    @DisplayName("filtering by status keeps the status filter as well as the buyer")
    void findByBuyerIdAndStatus_scopesToBoth() {
        assertThat(orderRepository.findByBuyerIdAndStatus(jane.getId(), OrderStatus.PENDING))
                .extracting(Order::getId)
                .containsExactly(pending.getId());
        assertThat(orderRepository.findByBuyerIdAndStatus(jane.getId(), OrderStatus.CANCELLED))
                .as("a status the buyer never used must not borrow another buyer's orders")
                .isEmpty();
    }

    @Test
    @DisplayName("the order history is newest first")
    void findByBuyerIdOrderByCreatedAtDesc_isNewestFirst() {
        Order newest = saveOrder(jane, OrderStatus.DELIVERED, "60.00", LocalDateTime.of(2026, 3, 1, 10, 0));

        assertThat(orderRepository.findByBuyerIdOrderByCreatedAtDesc(jane.getId()))
                .extracting(Order::getId)
                .containsExactly(newest.getId(), shipped.getId(), pending.getId());
    }

    @Test
    @DisplayName("an order loads for the buyer who placed it")
    void findByIdAndBuyerId_findsTheBuyersOwnOrder() {
        assertThat(orderRepository.findByIdAndBuyerId(pending.getId(), jane.getId())).contains(pending);
    }

    @Test
    @DisplayName("an order does not load for a different buyer, even with a valid id")
    void findByIdAndBuyerId_excludesAnotherBuyer() {
        assertThat(orderRepository.findByIdAndBuyerId(pending.getId(), john.getId()))
                .as("this is what stops one student opening another student's order by id")
                .isEmpty();
    }

    @Test
    @DisplayName("the existence check agrees with the load, so a 404 and a 403 stay consistent")
    void existsByIdAndBuyerId_matchesWhatFindByIdAndBuyerIdCanReturn() {
        assertThat(orderRepository.existsByIdAndBuyerId(pending.getId(), jane.getId())).isTrue();
        assertThat(orderRepository.existsByIdAndBuyerId(pending.getId(), john.getId())).isFalse();
        assertThat(orderRepository.existsByIdAndBuyerId(UUID.randomUUID(), jane.getId())).isFalse();
    }

    @Test
    @DisplayName("the dashboard counts are per buyer")
    void countByBuyerIdAndStatus_countsOneBuyersOrders() {
        assertThat(orderRepository.countByBuyerId(jane.getId())).isEqualTo(2);
        assertThat(orderRepository.countByBuyerIdAndStatus(jane.getId(), OrderStatus.PENDING)).isEqualTo(1);
        assertThat(orderRepository.countByBuyerIdAndStatus(jane.getId(), OrderStatus.DELIVERED)).isZero();
        assertThat(orderRepository.countByBuyerId(john.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("a cancelled order stops counting towards the active total")
    void countByBuyerIdAndStatus_excludesCancelledOrders() {
        saveOrder(jane, OrderStatus.CANCELLED, "30.00", LocalDateTime.of(2026, 3, 2, 10, 0));

        assertThat(orderRepository.countByBuyerIdAndStatus(jane.getId(), OrderStatus.CANCELLED)).isEqualTo(1);
        assertThat(orderRepository.countByBuyerId(jane.getId()))
                .as("the all-time total still includes it")
                .isEqualTo(3);
        assertThat(orderRepository.countByBuyerIdAndStatus(jane.getId(), OrderStatus.PENDING))
                .as("a cancellation must not push another order into a different bucket")
                .isEqualTo(1);
    }

    /**
     * Writes {@code created_at} directly rather than through the builder.
     *
     * <p>{@code Order.onCreate} is a {@code @PrePersist} that overwrites the field with
     * {@code LocalDateTime.now()}, so a builder-supplied timestamp is discarded and every fixture gets
     * the same instant to the microsecond. An ordering assertion written that way passes or fails
     * depending on the order the rows happened to be inserted in, which is worse than no test at all.
     */
    private void setCreatedAt(Order order, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update orders set created_at = ?1 where id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, order.getId())
                .executeUpdate();
        entityManager.clear();
    }

    private Order saveOrder(User buyer, OrderStatus status, String total, LocalDateTime createdAt) {
        Order saved = orderRepository.save(new Order.Builder()
                .setBuyer(buyer)
                .setStatus(status)
                .setTotalAmount(new BigDecimal(total))
                .build());
        setCreatedAt(saved, createdAt);
        return saved;
    }

    private User buildUser(String name) {
        return new User.Builder()
                .setName(name)
                .setEmail(name + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build();
    }
}