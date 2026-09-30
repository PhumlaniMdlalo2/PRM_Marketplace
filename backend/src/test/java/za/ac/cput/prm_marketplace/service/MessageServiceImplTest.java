package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private MessageServiceImpl messageService;

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Participant")
                .setEmail("p@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private Conversation buildConversation(User buyer, User seller) {
        return new Conversation.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setSeller(seller)
                .build();
    }

    @Test
    @DisplayName("create persists the message and touches the conversation")
    void create_savesAndTouchesConversation() {
        Conversation conversation = buildConversation(buildUser(UUID.randomUUID()), buildUser(UUID.randomUUID()));
        Message message = new Message.Builder()
                .setId(UUID.randomUUID())
                .setConversation(conversation)
                .setSender(buildUser(UUID.randomUUID()))
                .setBody("Is this available?")
                .build();

        when(messageRepository.save(message)).thenReturn(message);
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        assertThat(messageService.create(message)).isSameAs(message);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("read missing message returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(messageRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(messageService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing message")
    void update_missing_returnsNull() {
        Message message = new Message.Builder()
                .setId(UUID.randomUUID())
                .setConversation(buildConversation(buildUser(UUID.randomUUID()), buildUser(UUID.randomUUID())))
                .setSender(buildUser(UUID.randomUUID()))
                .setBody("Hi")
                .build();
        when(messageRepository.existsById(message.getId())).thenReturn(false);

        assertThat(messageService.update(message)).isNull();
    }

    @Test
    @DisplayName("delete reports false for an unknown message")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(messageRepository.existsById(id)).thenReturn(false);

        assertThat(messageService.delete(id)).isFalse();
    }

    @Test
    @DisplayName("getByConversation with null id returns empty")
    void getByConversation_withNull_returnsEmpty() {
        assertThat(messageService.getByConversation(null)).isEmpty();
    }

    @Test
    @DisplayName("send stores a trimmed SENT message and bumps the conversation")
    void send_persistsTrimmedMessage() {
        User buyer = buildUser(UUID.randomUUID());
        Conversation conversation = buildConversation(buyer, buildUser(UUID.randomUUID()));
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Message sent = messageService.send(conversation.getId(), buyer.getId(), "  Is this available?  ");

        assertThat(sent).isNotNull();
        assertThat(sent.getBody()).isEqualTo("Is this available?");
        assertThat(sent.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(sent.getSender()).isSameAs(buyer);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("send allows the seller as well as the buyer")
    void send_allowsSeller() {
        User seller = buildUser(UUID.randomUUID());
        Conversation conversation = buildConversation(buildUser(UUID.randomUUID()), seller);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(messageService.send(conversation.getId(), seller.getId(), "Yes it is"))
                .isNotNull();
    }

    @Test
    @DisplayName("send rejects non-participants")
    void send_rejectsOutsider() {
        Conversation conversation = buildConversation(buildUser(UUID.randomUUID()), buildUser(UUID.randomUUID()));
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThat(messageService.send(conversation.getId(), UUID.randomUUID(), "Hello"))
                .isNull();
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("send rejects a missing conversation, blank body or missing sender")
    void send_rejectsInvalidInput() {
        Conversation conversation = buildConversation(buildUser(UUID.randomUUID()), buildUser(UUID.randomUUID()));
        User buyer = conversation.getBuyer();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThat(messageService.send(conversation.getId(), buyer.getId(), null)).isNull();
        assertThat(messageService.send(conversation.getId(), buyer.getId(), "   ")).isNull();
        assertThat(messageService.send(conversation.getId(), null, "Hello")).isNull();

        UUID unknown = UUID.randomUUID();
        when(conversationRepository.findById(unknown)).thenReturn(Optional.empty());
        assertThat(messageService.send(unknown, buyer.getId(), "Hello")).isNull();
    }

    @Test
    @DisplayName("markRead delegates to the bulk update and ignores nulls")
    void markRead_delegates() {
        UUID conversationId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();

        when(messageRepository.markConversationRead(conversationId, readerId, MessageStatus.READ))
                .thenReturn(3);

        assertThat(messageService.markRead(conversationId, readerId)).isEqualTo(3);
        assertThat(messageService.markRead(null, readerId)).isZero();
        assertThat(messageService.markRead(conversationId, null)).isZero();
    }

    @Test
    @DisplayName("unreadCount counts everything that is not READ")
    void unreadCount_countsNonRead() {
        UUID conversationId = UUID.randomUUID();
        when(messageRepository.countByConversationIdAndStatusNot(conversationId, MessageStatus.READ))
                .thenReturn(2L);

        assertThat(messageService.unreadCount(conversationId)).isEqualTo(2L);
        assertThat(messageService.unreadCount(null)).isZero();
    }

    @Test
    @DisplayName("create without a conversation does not touch the conversation repository")
    void create_withoutConversation_doesNotTouchConversation() {
        Message message = new Message.Builder()
                .setId(UUID.randomUUID())
                .setSender(buildUser(UUID.randomUUID()))
                .setBody("Orphan")
                .build();
        when(messageRepository.save(message)).thenReturn(message);

        assertThat(messageService.create(message)).isSameAs(message);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("getAll delegates to the repository")
    void getAll_delegates() {
        List<Message> all = List.of();
        when(messageRepository.findAll()).thenReturn(all);

        assertThat(messageService.getAll()).isSameAs(all);
    }
}
