package za.ac.cput.prm_marketplace.json;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.domain.User;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The cycle-breaking annotations must be direction-specific: back-references are hidden
 * from responses but still accepted on input, and identifiers must survive a round trip
 * so that update endpoints that key off the body's id keep working.
 *
 * <p>Payment and OrderItem are the exceptions, and deliberately so. Both are updated through a
 * route that names the row in the path, and neither has a nested reference that needs an id from
 * a body, so there is nothing left for a client-supplied id to do except let a request claim to be
 * updating something it was not sent. Their ids are written out and ignored on the way in.
 */
@SpringBootTest
class EntityJsonRoundTripTest {

    @Autowired
    private ObjectMapper objectMapper;

    private User buildUser(String email) {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail(email)
                .setPasswordHash("super-secret-hash")
                .build();
    }

    @Test
    void paymentId_isWrittenButNotAcceptedFromAClient() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment.Builder()
                .setId(id)
                .setOrderId(UUID.randomUUID())
                .setUserId(UUID.randomUUID())
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.CARD)
                .setStatus(PaymentStatus.PENDING)
                .build();

        String json = objectMapper.writeValueAsString(payment);
        assertThat(json).containsOnlyOnce("\"id\"");

        // Payment is the exception to the round-trip rule: its update route names the payment in
        // the path, so nothing needs an id from the body, and accepting one would only give a
        // client a second way to name a payment. It is written out but ignored on the way in.
        Payment back = objectMapper.readValue(json, Payment.class);
        assertThat(back.getId()).isNull();
    }

    @Test
    void paymentIgnoresServerOwnedFieldsSuppliedByAClient() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID clientUserId = UUID.randomUUID();

        Payment submitted = objectMapper.readValue("""
                {
                  "id": "%s",
                  "orderId": "%s",
                  "userId": "%s",
                  "amount": 780.00,
                  "method": "CARD",
                  "status": "COMPLETED",
                  "transactionReference": "PAY-FORGED"
                }
                """.formatted(clientId, UUID.randomUUID(), clientUserId), Payment.class);

        assertThat(submitted.getId()).isNull();
        assertThat(submitted.getUserId()).isNull();
        assertThat(submitted.getStatus()).isNull();
        assertThat(submitted.getTransactionReference()).isNull();
    }

    @Test
    void orderId_survivesRoundTripAlongsideItems() {
        UUID id = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(id)
                .setBuyer(buildUser("buyer@example.com"))
                .build();

        String json = objectMapper.writeValueAsString(order);
        assertThat(objectMapper.readValue(json, Order.class).getId()).isEqualTo(id);
    }

    @Test
    void orderIgnoresClientSuppliedFulfillmentAndDeliveryEstimate() throws Exception {
        Order submitted = objectMapper.readValue("""
                {
                  "fulfillmentMethod": "DELIVERY",
                  "estimatedDeliveryDate": "2026-10-14"
                }
                """, Order.class);

        assertThat(submitted.getFulfillmentMethod()).isNull();
        assertThat(submitted.getEstimatedDeliveryDate()).isNull();
    }

    @Test
    void backReference_isHiddenFromOutputButAcceptedOnInput() {
        Product product = new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();

        ProductImage image = new ProductImage.Builder()
                .setId(UUID.randomUUID())
                .setProduct(product)
                .setImageUrl("https://cdn.example.com/book.jpg")
                .build();

        String json = objectMapper.writeValueAsString(image);
        assertThat(json).doesNotContain("\"product\"").contains(image.getImageUrl());

        // The stripped response cannot carry the reference, so check the request direction
        // with a payload that actually contains it.
        String requestJson = "{\"id\":\"" + image.getId() + "\",\"imageUrl\":\""
                + image.getImageUrl() + "\",\"sortOrder\":0,\"primary\":false,"
                + "\"product\":{\"id\":\"" + product.getId() + "\",\"name\":\"Textbook\"}}";

        ProductImage back = objectMapper.readValue(requestJson, ProductImage.class);
        assertThat(back.getProduct()).isNotNull();
        assertThat(back.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    void parentReference_isHiddenFromOutputButAcceptedOnInput() {
        UUID parentId = UUID.randomUUID();
        za.ac.cput.prm_marketplace.domain.BulletinPost post =
                new za.ac.cput.prm_marketplace.domain.BulletinPost.Builder()
                        .setId(UUID.randomUUID())
                        .setAuthor(buildUser("author@example.com"))
                        .setTitle("Selling a desk")
                        .setBody("Desk in good condition")
                        .build();

        Comment parent = new Comment.Builder()
                .setId(parentId)
                .setPost(post)
                .setAuthor(buildUser("a@example.com"))
                .setBody("Root")
                .build();
        Comment reply = new Comment.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setAuthor(buildUser("b@example.com"))
                .setBody("Reply")
                .setParent(parent)
                .build();

        String json = objectMapper.writeValueAsString(reply);
        assertThat(json).doesNotContain("\"parent\"");

        String requestJson = "{\"id\":\"" + reply.getId() + "\",\"body\":\"Reply\","
                + "\"parent\":{\"id\":\"" + parentId + "\",\"body\":\"Root\"}}";

        Comment back = objectMapper.readValue(requestJson, Comment.class);
        assertThat(back.getParent()).isNotNull();
        assertThat(back.getParent().getId()).isEqualTo(parentId);
    }

    @Test
    void addressOwnerAndOrderItemOrder_areHiddenButAccepted() {
        User owner = buildUser("owner@example.com");
        Address address = new Address.Builder()
                .setId(UUID.randomUUID())
                .setUser(owner)
                .setLine1("12 Main Road")
                .setCity("Cape Town")
                .build();
        String addressJson = objectMapper.writeValueAsString(address);
        assertThat(addressJson).doesNotContain("\"user\"");
        String addressRequest = "{\"id\":\"" + address.getId() + "\",\"line1\":\"12 Main Road\","
                + "\"user\":{\"id\":\"" + owner.getId() + "\"}}";
        // Read-only, not write-only: the owner is never taken from the request body. The service
        // assigns it from the token, so a body naming another account is discarded outright.
        assertThat(objectMapper.readValue(addressRequest, Address.class).getUser()).isNull();

        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildUser("b@example.com"))
                .build();
        OrderItem item = new OrderItem.Builder()
                .setId(UUID.randomUUID())
                .setOrder(order)
                .setProduct(new Product.Builder().id(UUID.randomUUID()).name("P").build())
                .setQuantity(1)
                .setPriceAtPurchase(new BigDecimal("1.00"))
                .build();
        String itemJson = objectMapper.writeValueAsString(item);
        assertThat(itemJson).doesNotContain("\"order\"");
        String itemRequest = "{\"id\":\"" + item.getId() + "\",\"quantity\":1,"
                + "\"order\":{\"id\":\"" + order.getId() + "\"},"
                + "\"product\":{\"id\":\"" + item.getProduct().getId() + "\"}}";
        assertThat(objectMapper.readValue(itemRequest, OrderItem.class).getOrder()).isNotNull();
    }

    @Test
    void passwordHashIsNeverWrittenButUserIdIsStillReadable() {
        UUID userId = UUID.randomUUID();
        User user = new User.Builder()
                .setId(userId)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("super-secret-hash")
                .build();

        String json = objectMapper.writeValueAsString(user);
        assertThat(json).doesNotContain("super-secret-hash").doesNotContain("passwordHash");
        assertThat(json).contains(userId.toString());
    }

    @Test
    void orderGraph_stillSerialisesWithoutRecursion() {
        User buyer = buildUser("buyer@example.com");
        Product product = new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();
        product.addImage(new ProductImage.Builder()
                .setId(UUID.randomUUID())
                .setProduct(product)
                .setImageUrl("https://cdn.example.com/book.jpg")
                .build());

        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setShippingAddress(new Address.Builder()
                        .setId(UUID.randomUUID())
                        .setUser(buyer)
                        .setLine1("12 Main Road")
                        .build())
                .build();
        order.addItem(new OrderItem.Builder()
                .setId(UUID.randomUUID())
                .setOrder(order)
                .setProduct(product)
                .setQuantity(2)
                .setPriceAtPurchase(new BigDecimal("250.00"))
                .build());

        assertThatCode(() -> objectMapper.writeValueAsString(order)).doesNotThrowAnyException();
        assertThat(objectMapper.writeValueAsString(order)).doesNotContain("super-secret-hash");
    }
}
