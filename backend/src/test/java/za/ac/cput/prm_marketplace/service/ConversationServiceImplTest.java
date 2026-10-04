package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private ConversationServiceImpl service;

    private UUID buyerId;
    private UUID sellerId;
    private UUID outsiderId;
    private UUID conversationId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        service = new ConversationServiceImpl(conversationRepository, userRepository, productRepository);
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("starting a thread makes the requester the buyer and the other party the seller")
    void getOrCreate_assignsRequesterAsBuyer() {
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buildUser(buyerId)));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(buildUser(sellerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct()));
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Conversation created = service.getOrCreate(buyerId, sellerId, productId);

        assertThat(created).isNotNull();
        assertThat(created.getBuyer().getId()).isEqualTo(buyerId);
        assertThat(created.getSeller().getId()).isEqualTo(sellerId);
    }

    @Test
    @DisplayName("an existing thread is reused rather than duplicated")
    void getOrCreate_reusesExistingThread() {
        Conversation existing = buildConversation(buyerId, sellerId);
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buildUser(buyerId)));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(buildUser(sellerId)));
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductIsNull(buyerId, sellerId))
                .thenReturn(Optional.of(existing));

        assertThat(service.getOrCreate(buyerId, sellerId, null)).isSameAs(existing);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("a conversation with yourself is refused")
    void getOrCreate_refusesSelfConversation() {
        assertThat(service.getOrCreate(buyerId, buyerId, productId)).isNull();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("starting a thread requires both accounts to exist")
    void getOrCreate_requiresBothUsers() {
        when(userRepository.findById(buyerId)).thenReturn(Optional.empty());
        assertThat(service.getOrCreate(buyerId, sellerId, productId)).isNull();

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buildUser(buyerId)));
        when(userRepository.findById(sellerId)).thenReturn(Optional.empty());
        assertThat(service.getOrCreate(buyerId, sellerId, productId)).isNull();

        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("an unknown product id yields a thread with no product attached")
    void getOrCreate_unknownProductAttachesNothing() {
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buildUser(buyerId)));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(buildUser(sellerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Conversation created = service.getOrCreate(buyerId, sellerId, productId);

        assertThat(created).isNotNull();
        assertThat(created.getProduct()).isNull();
    }

    @Test
    @DisplayName("both participants can read the thread")
    void read_allowsParticipants() {
        Conversation conversation = buildConversation(buyerId, sellerId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThat(service.read(conversationId, buyerId)).isSameAs(conversation);
        assertThat(service.read(conversationId, sellerId)).isSameAs(conversation);
    }

    @Test
    @DisplayName("a third party cannot read the thread")
    void read_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(buyerId, sellerId)));

        assertThat(service.read(conversationId, outsiderId)).isNull();
    }

    @Test
    @DisplayName("reading a missing thread returns null")
    void read_missingReturnsNull() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThat(service.read(conversationId, buyerId)).isNull();
        assertThat(service.read(null, buyerId)).isNull();
    }

    @Test
    @DisplayName("a participant can delete the thread")
    void delete_allowsParticipant() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(buyerId, sellerId)));

        assertThat(service.delete(conversationId, buyerId)).isTrue();
        verify(conversationRepository).deleteById(conversationId);
    }

    @Test
    @DisplayName("an outsider cannot delete the thread")
    void delete_refusesOutsider() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(buyerId, sellerId)));

        assertThat(service.delete(conversationId, outsiderId)).isFalse();
        verify(conversationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("the thread list is scoped to the caller")
    void getForUser_scopesToTheRequester() {
        Conversation conversation = buildConversation(buyerId, sellerId);
        when(conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(buyerId, buyerId))
                .thenReturn(List.of(conversation));

        assertThat(service.getForUser(buyerId)).containsExactly(conversation);
        assertThat(service.getForUser(null)).isEmpty();
    }

    @Test
    @DisplayName("the unread count is scoped to the caller")
    void unreadCount_scopesToTheRequester() {
        when(conversationRepository.countUnreadConversations(buyerId,
                za.ac.cput.prm_marketplace.domain.MessageStatus.READ)).thenReturn(4L);

        assertThat(service.unreadCount(buyerId)).isEqualTo(4L);
        assertThat(service.unreadCount(null)).isZero();
    }

    @Test
    @DisplayName("participantIds reports both parties and nothing for a missing thread")
    void participantIds_reportsBothParties() {
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(buildConversation(buyerId, sellerId)));

        assertThat(service.participantIds(conversationId)).containsExactly(buyerId, sellerId);
        assertThat(service.participantIds(UUID.randomUUID())).isEmpty();
        assertThat(service.participantIds(null)).isEmpty();
    }

    @Test
    @DisplayName("a thread whose product is re-read keeps the stored product reference")
    void getOrCreate_reusesStoredProduct() {
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buildUser(buyerId)));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(buildUser(sellerId)));
        Product product = buildProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(conversationRepository.findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Conversation created = service.getOrCreate(buyerId, sellerId, productId);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isSameAs(product);
        assertThat(created.getProduct().getId()).isEqualTo(productId);
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Product buildProduct() {
        return new Product.Builder().id(productId).name("Widget").build();
    }

    private Conversation buildConversation(UUID buyer, UUID seller) {
        return new Conversation.Builder()
                .setId(conversationId)
                .setBuyer(buildUser(buyer))
                .setSeller(buildUser(seller))
                .build();
    }
}