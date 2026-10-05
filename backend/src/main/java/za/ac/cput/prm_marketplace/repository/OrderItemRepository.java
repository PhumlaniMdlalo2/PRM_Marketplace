package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.OrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Line items are owned through their order's buyer, so the owner-scoped queries reach through
 * {@code order.buyer} rather than taking a buyer id directly: the item itself stores no owner.
 *
 * <p>These exist so the API never has to read a line item by bare id. Every row on this table is
 * part of somebody's order, and an unguarded {@code findById} would hand any authenticated caller
 * a look at another account's purchases.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByProductId(UUID productId);

    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    /** Null when the line item does not exist or its order belongs to somebody else. */
    Optional<OrderItem> findByIdAndOrderBuyerId(UUID id, UUID buyerId);

    List<OrderItem> findByOrderIdAndOrderBuyerIdOrderByCreatedAtAsc(UUID orderId, UUID buyerId);

    boolean existsByIdAndOrderBuyerId(UUID id, UUID buyerId);

    /**
     * True when the given vendor account sells at least one of the products on this order.
     *
     * <p>A seller may only act on an order that contains something they actually sell. Without this
     * check, holding a vendor account is enough to advance every order in the system, because the
     * order is located by id alone. The test walks the whole ownership chain in one query —
     * order to line item, to product, to that product's vendor, to the vendor's user — so the
     * decision is made by the database rather than by loading the graph in Java.
     */
    @Query("""
            select count(oi) > 0
            from OrderItem oi
            where oi.order.id = :orderId
              and oi.product.vendor.user.id = :vendorUserId
            """)
    boolean existsByOrderIdAndVendorUserId(@Param("orderId") UUID orderId,
                                           @Param("vendorUserId") UUID vendorUserId);

    /**
     * True when this buyer has actually bought this product.
     *
     * <p>Reviews are the marketplace's trust signal, so a review from somebody who never bought the
     * thing says nothing about the seller. Without this, any authenticated account can review any
     * listing and the average rating is worth nothing.
     *
     * <p>CANCELLED and REFUNDED are excluded on purpose. Both mean the money went back: a cancelled
     * order had its stock returned to the catalogue, and a refunded one was returned to the buyer.
     * Either way there is no longer a purchase, and a review left behind would keep claiming there
     * is. PENDING through DELIVERED all count, so a buyer can review as soon as checkout completes
     * rather than having to wait for delivery.
     *
     * <p>As with the vendor check above, the whole ownership chain is walked in the database rather
     * than by loading the graph in Java.
     */
    @Query("""
            select count(oi) > 0
            from OrderItem oi
            where oi.product.id = :productId
              and oi.order.buyer.id = :buyerId
              and oi.order.status not in (za.ac.cput.prm_marketplace.domain.OrderStatus.CANCELLED,
                                          za.ac.cput.prm_marketplace.domain.OrderStatus.REFUNDED)
            """)
    boolean existsByProductIdAndBuyerId(@Param("productId") UUID productId,
                                        @Param("buyerId") UUID buyerId);
}