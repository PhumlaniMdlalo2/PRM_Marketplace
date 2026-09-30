package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Product;

import java.math.BigDecimal;

public class OrderItemFactory {

    public static OrderItem createOrderItem(Order order, Product product,
                                             int quantity, BigDecimal priceAtPurchase) {

        if (order == null) {
            return null;
        }

        if (product == null) {
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
                .setOrder(order)
                .setProduct(product)
                .setQuantity(quantity)
                .setPriceAtPurchase(priceAtPurchase)
                .build();
    }

    public static OrderItem createOrderItemForProduct(Order order, Product product, int quantity) {
        if (product == null) {
            return null;
        }
        return createOrderItem(order, product, quantity, product.getPrice());
    }
}