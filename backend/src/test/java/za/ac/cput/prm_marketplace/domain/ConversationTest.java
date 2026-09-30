package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationTest {

    private User buildUser(String email) {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("User " + email)
                .setEmail(email)
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private Conversation.Builder builder() {
        return new Conversation.Builder()
                .setBuyer(buildUser("buyer@example.com"))
                .setSeller(buildUser("seller@example.com"));
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User buyer = buildUser("buyer@example.com");
        User seller = buildUser("seller@example.com");
        Product product = new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();

        Conversation conversation = new Conversation.Builder()
                .setId(id)
                .setBuyer(buyer)
                .setSeller(seller)
                .setProduct(product)
                .build();

        assertThat(conversation.getId()).isEqualTo(id);
        assertThat(conversation.getBuyer()).isEqualTo(buyer);
        assertThat(conversation.getSeller()).isEqualTo(seller);
        assertThat(conversation.getProduct()).isEqualTo(product);
    }

    @Test
    void productIsOptionalForGeneralInquiries() {
        Conversation conversation = builder().build();

        assertThat(conversation.getProduct()).isNull();
    }

    @Test
    void involvesReturnsTrueForBothParticipants() {
        User buyer = buildUser("buyer@example.com");
        User seller = buildUser("seller@example.com");
        User stranger = buildUser("stranger@example.com");
        Conversation conversation = new Conversation.Builder()
                .setBuyer(buyer)
                .setSeller(seller)
                .build();

        assertThat(conversation.involves(buyer)).isTrue();
        assertThat(conversation.involves(seller)).isTrue();
        assertThat(conversation.involves(stranger)).isFalse();
    }

    @Test
    void counterpartOfSwapsBuyerAndSeller() {
        User buyer = buildUser("buyer@example.com");
        User seller = buildUser("seller@example.com");
        Conversation conversation = new Conversation.Builder()
                .setBuyer(buyer)
                .setSeller(seller)
                .build();

        assertThat(conversation.counterpartOf(buyer)).isEqualTo(seller);
        assertThat(conversation.counterpartOf(seller)).isEqualTo(buyer);
    }

    @Test
    void counterpartOfReturnsNullForNonParticipants() {
        User stranger = buildUser("stranger@example.com");
        Conversation conversation = builder().build();

        assertThat(conversation.counterpartOf(stranger)).isNull();
    }

    @Test
    void touchAdvancesLastMessageAt() {
        Conversation conversation = builder().build();
        conversation.touch();

        assertThat(conversation.getLastMessageAt()).isNotNull();
    }

    @Test
    void touchOnANewConversationStampsLastMessageAt() {
        Conversation conversation = builder().build();

        assertThat(conversation.getLastMessageAt()).isNull();

        conversation.touch();
        assertThat(conversation.getLastMessageAt()).isNotNull();
    }
}
