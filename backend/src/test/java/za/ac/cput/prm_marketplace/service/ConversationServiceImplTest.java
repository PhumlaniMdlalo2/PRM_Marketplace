package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.MessageRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Participant")
                .setEmail("p@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private Conversation buildConversation(User buyer, User seller, Product product) {
        return new Conversation.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setSeller(seller)
                .setProduct(product)
                .build();
    }

    @Test
    @DisplayName("create persists and returns the conversation")
    void create_saves() {
        Conversation conversation = buildConversation(buildUser(), buildUser(), null);
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        assertThat(conversationService.create(conversation)).isSameAs(conversation);
    }

    @Test
    @DisplayName("create with null returns null without touching the repository")
    void create_withNull_returnsNull() {
        assertThat(conversationService.create(null)).isNull();
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("read returns null for a missing conversation")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(conversationService.read(id)).isNull();
    }

    @Test
    @DisplayName("read with null id returns null without querying")
    void read_withNullId_returnsNull() {
        assertThat(conversationService.read(null)).isNull();
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update requires an existing id")
    void update_missing_returnsNull() {
        Conversation conversation = buildConversation(buildUser(), buildUser(), null);
        when(conversationRepository.existsById(conversation.getId())).thenReturn(false);

        assertThat(conversationService.update(conversation)).isNull();
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update saves when the conversation exists")
    void update_existing_saves() {
        Conversation conversation = buildConversation(buildUser(), buildUser(), null);
        when(conversationRepository.existsById(conversation.getId())).thenReturn(true);
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        assertThat(conversationService.update(conversation)).isSameAs(conversation);
    }

    @Test
    @DisplayName("delete reports false when the conversation is absent")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.existsById(id)).thenReturn(false);

        assertThat(conversationService.delete(id)).isFalse();
        verify(conversationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete removes an existing conversation")
    void delete_existing_returnsTrue() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.existsById(id)).thenReturn(true);

        assertThat(conversationService.delete(id)).isTrue();
        verify(conversationRepository).deleteById(id);
    }

    @Test
    @DisplayName("getForUser returns conversations for either side of the trade")
    void getForUser_queriesBothSides() {
        UUID userId = UUID.randomUUID();
        List<Conversation> expected = List.of(buildConversation(buildUser(), buildUser(), null));
        when(conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(userId, userId))
                .thenReturn(expected);

        assertThat(conversationService.getForUser(userId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("getForUser with null id returns an empty list")
    void getForUser_withNullId_returnsEmpty() {
        assertThat(conversationService.getForUser(null)).isEmpty();
    }

    @Test
    @DisplayName("getOrCreate reuses an existing conversation for the same product")
    void getOrCreate_reusesExisting() {
        User buyer = buildUser();
        User seller = buildUser();
        Product product = new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .build();

        Conversation existing = buildConversation(buyer, seller, product);
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductId(
                buyer.getId(), seller.getId(), product.getId()))
                .thenReturn(Optional.of(existing));

        assertThat(conversationService.getOrCreate(buyer, seller, product)).isSameAs(existing);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate matches a general conversation when no product is given")
    void getOrCreate_reusesGeneralConversation() {
        User buyer = buildUser();
        User seller = buildUser();

        Conversation existing = buildConversation(buyer, seller, null);
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductIsNull(
                buyer.getId(), seller.getId()))
                .thenReturn(Optional.of(existing));

        assertThat(conversationService.getOrCreate(buyer, seller, null)).isSameAs(existing);
    }

    @Test
    @DisplayName("getOrCreate opens a new conversation when none exists")
    void getOrCreate_createsWhenAbsent() {
        User buyer = buildUser();
        User seller = buildUser();
        Product product = new Product.Builder().id(UUID.randomUUID()).name("Textbook").build();

        when(conversationRepository.findByBuyerIdAndSellerIdAndProductId(
                buyer.getId(), seller.getId(), product.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation created = conversationService.getOrCreate(buyer, seller, product);

        assertThat(created).isNotNull();
        assertThat(created.getBuyer()).isSameAs(buyer);
        assertThat(created.getSeller()).isSameAs(seller);
        assertThat(created.getProduct()).isSameAs(product);
    }

    @Test
    @DisplayName("getOrCreate refuses to open a conversation with yourself")
    void getOrCreate_withSelf_returnsNull() {
        User user = buildUser();
        assertThat(conversationService.getOrCreate(user, user, null)).isNull();
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate requires both participants")
    void getOrCreate_withMissingParty_returnsNull() {
        assertThat(conversationService.getOrCreate(null, buildUser(), null)).isNull();
        assertThat(conversationService.getOrCreate(buildUser(), null, null)).isNull();
    }

    @Test
    @DisplayName("readForParticipant allows the buyer")
    void readForParticipant_allowsBuyer() {
        User buyer = buildUser();
        Conversation conversation = buildConversation(buyer, buildUser(), null);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThat(conversationService.readForParticipant(conversation.getId(), buyer.getId()))
                .isSameAs(conversation);
    }

    @Test
    @DisplayName("readForParticipant allows the seller")
    void readForParticipant_allowsSeller() {
        User seller = buildUser();
        Conversation conversation = buildConversation(buildUser(), seller, null);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThat(conversationService.readForParticipant(conversation.getId(), seller.getId()))
                .isSameAs(conversation);
    }

    @Test
    @DisplayName("readForParticipant hides the conversation from outsiders")
    void readForParticipant_deniesOutsider() {
        Conversation conversation = buildConversation(buildUser(), buildUser(), null);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThat(conversationService.readForParticipant(conversation.getId(), UUID.randomUUID()))
                .isNull();
    }

    @Test
    @DisplayName("unreadCount with null id is zero")
    void unreadCount_withNullId_isZero() {
        assertThat(conversationService.unreadCount(null)).isZero();
    }
}
