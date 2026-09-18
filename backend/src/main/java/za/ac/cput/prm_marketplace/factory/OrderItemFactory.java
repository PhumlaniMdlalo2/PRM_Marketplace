package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemFactory {

    public static OrderItem createOrderItem(UUID orderId, UUID productId,
                                            int quantity, BigDecimal priceAtPurchase) {

        if (orderId == null) {
            return null;
        }

        if (productId == null) {
            return null;
        }

        if (quantity <= 0) {
            return null;
        }

        if (priceAtPurchase == null ||
                priceAtPurchase.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        return new OrderItem.Builder()
                .setOrderId(orderId)
                .setProductId(productId)
                .setQuantity(quantity)
                .setPriceAtPurchase(priceAtPurchase)
                .build();
    }
}