package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.MessageStatus;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.MessageRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    private MessageServiceImpl service;

    private UUID senderId;
    private UUID recipientId;
    private UUID outsiderId;
    private UUID conversationId;
    private UUID messageId;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageRepository, conversationRepository, userRepository);
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
    }

    @Test
    @DisplayName("a participant sees the thread")
    void getByConversation_allowsParticipant() {
        Message message = buildMessage(senderId);
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId))
                .thenReturn(List.of(message));

        assertThat(service.getByConversation(conversationId, recipientId)).containsExactly(message);
    }

    @Test
    @DisplayName("a third party gets nothing, and no query is run against the thread")
    void getByConversation_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.getByConversation(conversationId, outsiderId)).isEmpty();

        verify(messageRepository, never()).findByConversationIdOrderBySentAtAsc(any());
    }

    @Test
    @DisplayName("a missing thread yields no messages")
    void getByConversation_missingThread() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThat(service.getByConversation(conversationId, senderId)).isEmpty();
    }

    @Test
    @DisplayName("a participant sends a message as themselves")
    void send_storesTheAuthoredMessage() {
        Conversation conversation = buildConversation(senderId, recipientId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(buildUser(senderId)));
        when(messageRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Message sent = service.send(conversationId, senderId, "  Is this available?  ");

        assertThat(sent).isNotNull();
        assertThat(sent.getSender().getId()).isEqualTo(senderId);
        // The body is trimmed so a leading space cannot disguise an empty message in the UI.
        assertThat(sent.getBody()).isEqualTo("Is this available?");
        assertThat(sent.getStatus()).isEqualTo(MessageStatus.SENT);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("a third party cannot post into the thread")
    void send_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.send(conversationId, outsiderId, "hello")).isNull();

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("an empty body is refused before the thread is touched")
    void send_refusesBlankBody() {
        assertThat(service.send(conversationId, senderId, null)).isNull();
        assertThat(service.send(conversationId, senderId, "   ")).isNull();

        verify(conversationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("sending into a missing thread returns null")
    void send_missingThread() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThat(service.send(conversationId, senderId, "hello")).isNull();
    }

    @Test
    @DisplayName("marking read only touches messages the reader did not write")
    void markRead_excludesOwnMessages() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));
        when(messageRepository.markConversationRead(conversationId, recipientId, MessageStatus.READ))
                .thenReturn(3);

        assertThat(service.markRead(conversationId, recipientId)).isEqualTo(3);
    }

    @Test
    @DisplayName("a third party cannot mark somebody else's messages as read")
    void markRead_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.markRead(conversationId, outsiderId)).isZero();

        verify(messageRepository, never()).markConversationRead(any(), any(), any());
    }

    @Test
    @DisplayName("marking read needs a thread and a reader")
    void markRead_rejectsNulls() {
        assertThat(service.markRead(null, recipientId)).isZero();
        assertThat(service.markRead(conversationId, null)).isZero();
    }

    @Test
    @DisplayName("the unread count excludes the caller's own messages")
    void unreadCount_excludesOwnMessages() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));
        when(messageRepository.countByConversationIdAndStatusNotAndSender_IdNot(
                conversationId, MessageStatus.READ, recipientId)).thenReturn(2L);

        assertThat(service.unreadCount(conversationId, recipientId)).isEqualTo(2L);
    }

    @Test
    @DisplayName("a third party is told there is nothing unread")
    void unreadCount_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.unreadCount(conversationId, outsiderId)).isZero();
    }

    @Test
    @DisplayName("a participant can read a single message from the thread")
    void read_allowsParticipant() {
        Message message = buildMessage(senderId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.read(messageId, recipientId)).isSameAs(message);
    }

    @Test
    @DisplayName("guessing a message id does not reach a thread the caller is not in")
    void read_refusesOutsider() {
        Message message = buildMessage(senderId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(senderId, recipientId)));

        assertThat(service.read(messageId, outsiderId)).isNull();
    }

    @Test
    @DisplayName("reading a missing message or id returns null")
    void read_missingReturnsNull() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThat(service.read(messageId, senderId)).isNull();
        assertThat(service.read(null, senderId)).isNull();
    }

    @Test
    @DisplayName("a message with no conversation cannot be read")
    void read_withoutConversationReturnsNull() {
        Message orphan = new Message.Builder().setId(messageId).setBody("orphan").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(orphan));

        assertThat(service.read(messageId, senderId)).isNull();
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Conversation buildConversation(UUID buyer, UUID seller) {
        return new Conversation.Builder()
                .setId(conversationId)
                .setBuyer(buildUser(buyer))
                .setSeller(buildUser(seller))
                .build();
    }

    private Message buildMessage(UUID sender) {
        return new Message.Builder()
                .setId(messageId)
                .setConversation(buildConversation(senderId, recipientId))
                .setSender(buildUser(sender))
                .setBody("Is this still available?")
                .build();
    }
}