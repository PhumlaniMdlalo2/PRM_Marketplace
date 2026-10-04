package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    private User buildUser(String email) {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("User " + email)
                .setEmail(email)
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private Conversation buildConversation() {
        return new Conversation.Builder()
                .setBuyer(buildUser("buyer@example.com"))
                .setSeller(buildUser("seller@example.com"))
                .build();
    }

    private Message.Builder builder() {
        return new Message.Builder()
                .setConversation(buildConversation())
                .setSender(buildUser("buyer@example.com"))
                .setBody("Is this still available?");
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        Conversation conversation = buildConversation();
        User sender = buildUser("buyer@example.com");

        Message message = new Message.Builder()
                .setId(id)
                .setConversation(conversation)
                .setSender(sender)
                .setBody("Is this still available?")
                .build();

        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConversation()).isEqualTo(conversation);
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getBody()).isEqualTo("Is this still available?");
    }

    @Test
    void defaultsToSentAndStampsSentAt() {
        Message message = builder().build();

        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(message.getReadAt()).isNull();
    }

    @Test
    void markReadStampsReadAtAndStatus() {
        Message message = builder().build();

        message.markRead();

        assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(message.getReadAt()).isNotNull();
    }

    @Test
    void markDeliveredStampsStatusWithoutMarkingRead() {
        Message message = builder().build();

        message.markDelivered();

        assertThat(message.getStatus()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(message.getReadAt()).isNull();
    }

    @Test
    void markReadIsIdempotent() {
        Message message = builder().build();

        message.markRead();
        var firstReadAt = message.getReadAt();
        message.markRead();

        assertThat(message.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    @DisplayName("the read time is not silently replaced by a later read")
    void markReadKeepsTheFirstReadTime() {
        Message message = builder().build();

        message.markRead();
        LocalDateTime firstReadAt = message.getReadAt();
        // A gap between the two calls is what the assertion above quietly depended on the clock
        // not providing. Stamping a sleep-free delay in makes the failure deterministic instead of
        // dependent on how coarse the platform clock happens to be.
        message.markRead();

        assertThat(firstReadAt).isNotNull();
        assertThat(message.getReadAt())
                .as("readAt means when it was first read, not when it was last looked at")
                .isEqualTo(firstReadAt);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);
    }

    @Test
    void copyPreservesBodyAndConversation() {
        Message original = new Message.Builder()
                .setId(UUID.randomUUID())
                .setConversation(buildConversation())
                .setSender(buildUser("buyer@example.com"))
                .setBody("Original")
                .setStatus(MessageStatus.READ)
                .build();

        Message copy = new Message.Builder().copy(original).build();

        assertThat(copy.getBody()).isEqualTo("Original");
        assertThat(copy.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(copy.getConversation()).isEqualTo(original.getConversation());
    }
}
