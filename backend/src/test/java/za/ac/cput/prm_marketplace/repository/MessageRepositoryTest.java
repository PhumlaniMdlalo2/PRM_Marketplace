package za.ac.cput.prm_marketplace.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.MessageStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the message and conversation queries against a real database. The mocked service tests
 * cannot show whether the JPQL actually executes, and both of these queries were previously
 * wrong in ways only a real run would surface: marking a conversation read left {@code read_at}
 * null, and the unread tally reported every conversation a user took part in.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MessageRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;
    private User john;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane", "jane@example.com"));
        john = userRepository.save(buildUser("john", "john@example.com"));

        conversation = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .build());
    }

    private User buildUser(String name, String email) {
        return new User.Builder()
                .setName(name)
                .setEmail(email + "-" + UUID.randomUUID())
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build();
    }

    private Message saveMessage(User sender, String body, MessageStatus status) {
        return messageRepository.save(new Message.Builder()
                .setConversation(conversation)
                .setSender(sender)
                .setBody(body)
                .setStatus(status)
                .build());
    }

    @Test
    @DisplayName("marking a conversation read records when each message was read")
    void markConversationRead_recordsReadTimestamp() {
        Message fromJohn = saveMessage(john, "Is the desk still available?", MessageStatus.SENT);
        Message fromJane = saveMessage(jane, "Yes it is", MessageStatus.SENT);

        int updated = messageRepository.markConversationRead(
                conversation.getId(), jane.getId(), MessageStatus.READ);

        assertThat(updated).isEqualTo(1);

        // The bulk update bypasses the persistence context, so re-read to see what was stored.
        entityManager.clear();
        Message reloadedJohnMessage = messageRepository.findById(fromJohn.getId()).orElseThrow();
        Message reloadedJaneMessage = messageRepository.findById(fromJane.getId()).orElseThrow();

        assertThat(reloadedJohnMessage.getReadAt()).isNotNull();
        // The reader's own message is never part of the update.
        assertThat(reloadedJaneMessage.getReadAt()).isNull();
        assertThat(reloadedJohnMessage.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(reloadedJaneMessage.getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    @DisplayName("marking read a second time changes nothing")
    void markConversationRead_isIdempotent() {
        saveMessage(john, "Is the desk still available?", MessageStatus.SENT);

        messageRepository.markConversationRead(conversation.getId(), jane.getId(), MessageStatus.READ);
        int secondPass = messageRepository.markConversationRead(
                conversation.getId(), jane.getId(), MessageStatus.READ);

        assertThat(secondPass).isZero();
    }

    @Test
    @DisplayName("only conversations with unread incoming messages are counted")
    void countUnreadConversations_ignoresFullyReadAndOwnMessages() {
        Conversation readThread = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .build());

        // Fully unread: one incoming SENT message.
        saveMessage(john, "Still available?", MessageStatus.SENT);

        // Fully read: everything already marked READ.
        saveMessage(john, "Read already", MessageStatus.READ);

        // Only the reader's own outgoing message, which is never unread for the reader.
        messageRepository.save(new Message.Builder()
                .setConversation(readThread)
                .setSender(jane)
                .setBody("Sent, not read by me")
                .setStatus(MessageStatus.SENT)
                .build());

        long unread = conversationRepository.countUnreadConversations(jane.getId(), MessageStatus.READ);

        assertThat(unread).isEqualTo(1);
    }

    @Test
    @DisplayName("a conversation with several unread messages is counted once")
    void countUnreadConversations_countsDistinctConversations() {
        saveMessage(john, "First", MessageStatus.SENT);
        saveMessage(john, "Second", MessageStatus.SENT);
        saveMessage(john, "Third", MessageStatus.DELIVERED);

        assertThat(conversationRepository.countUnreadConversations(jane.getId(), MessageStatus.READ))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a user with no unread messages has nothing to read")
    void countUnreadConversations_isZeroWhenNothingIsUnread() {
        assertThat(conversationRepository.countUnreadConversations(john.getId(), MessageStatus.READ))
                .isZero();
    }

    @Test
    @DisplayName("a thread's messages are removed, which is what lets the thread itself be deleted")
    void deleteByConversationId_removesOnlyThatThreadsMessages() {
        Conversation otherThread = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .build());
        saveMessage(john, "Still available?", MessageStatus.SENT);
        saveMessage(jane, "Yes it is", MessageStatus.SENT);
        Message someoneElses = messageRepository.save(new Message.Builder()
                .setConversation(otherThread)
                .setSender(jane)
                .setBody("Different thread")
                .setStatus(MessageStatus.SENT)
                .build());

        messageRepository.deleteByConversationId(conversation.getId());

        // The foreign key from messages to conversations is non-nullable, so a thread with history
        // could not be deleted at all without this. It only ever showed up once somebody had written
        // in the thread: an empty thread deleted cleanly and made the endpoint look working.
        entityManager.flush();
        assertThat(conversationRepository.findById(conversation.getId())).isPresent();
        assertThat(messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId())).isEmpty();
        assertThat(messageRepository.findById(someoneElses.getId())).isPresent();
    }

    @Test
    @DisplayName("deleting a thread and its messages together leaves nothing behind")
    void deletingAThreadWithMessages_succeeds() {
        saveMessage(john, "Is the desk still available?", MessageStatus.SENT);
        saveMessage(jane, "Yes it is", MessageStatus.DELIVERED);

        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.deleteById(conversation.getId());
        entityManager.flush();

        assertThat(conversationRepository.findById(conversation.getId())).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }
}