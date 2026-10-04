package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.MessageStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.MessageRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements IConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final MessageRepository messageRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   ProductRepository productRepository,
                                   MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public Conversation getOrCreate(UUID requesterId, UUID otherPartyId, UUID productId) {
        if (requesterId == null || otherPartyId == null || requesterId.equals(otherPartyId)) {
            return null;
        }
        User requester = userRepository.findById(requesterId).orElse(null);
        User otherParty = userRepository.findById(otherPartyId).orElse(null);
        if (requester == null || otherParty == null) {
            return null;
        }
        // Re-read the product so a client cannot attach an unsaved or fabricated row.
        Product product = productId == null ? null : productRepository.findById(productId).orElse(null);

        Optional<Conversation> existing = findExisting(requesterId, otherPartyId, productId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // The requester is always the buyer, so the caller cannot nominate the other side's role
        // or insert themselves as the seller.
        return conversationRepository.save(new Conversation.Builder()
                .setBuyer(requester)
                .setSeller(otherParty)
                .setProduct(product)
                .build());
    }

    @Override
    public Conversation read(UUID id, UUID requesterId) {
        Conversation conversation = find(id);
        return isParticipant(conversation, requesterId) ? conversation : null;
    }

    /**
     * Deletes a thread the caller is part of, and everything in it.
     *
     * <p>The messages go first. {@code messages.conversation_id} is a non-nullable foreign key onto
     * this row, so deleting a conversation that has history raises a constraint violation instead of
     * deleting anything — and it raises it as a 500, because a database constraint is not something
     * the handler recognises. The failure only ever appears on a thread somebody has actually written
     * in, so an empty thread deleted fine and made this look working until it wasn't.
     */
    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (read(id, requesterId) == null) {
            return false;
        }
        messageRepository.deleteByConversationId(id);
        conversationRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Conversation> getForUser(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(
                requesterId, requesterId);
    }

    @Override
    public long unreadCount(UUID requesterId) {
        if (requesterId == null) {
            return 0L;
        }
        return conversationRepository.countUnreadConversations(requesterId, MessageStatus.READ);
    }

    @Override
    public List<UUID> participantIds(UUID conversationId) {
        Conversation conversation = find(conversationId);
        if (conversation == null) {
            return List.of();
        }
        return List.of(conversation.getBuyer().getId(), conversation.getSeller().getId());
    }

    private Optional<Conversation> findExisting(UUID buyerId, UUID sellerId, UUID productId) {
        if (productId == null) {
            return conversationRepository.findByBuyerIdAndSellerIdAndProductIsNull(buyerId, sellerId);
        }
        return conversationRepository.findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId);
    }

    private Conversation find(UUID id) {
        if (id == null) {
            return null;
        }
        return conversationRepository.findById(id).orElse(null);
    }

    private boolean isParticipant(Conversation conversation, UUID userId) {
        return conversation != null
                && userId != null
                && (matches(conversation.getBuyer(), userId) || matches(conversation.getSeller(), userId));
    }

    private boolean matches(User party, UUID userId) {
        return party != null && party.getId() != null && party.getId().equals(userId);
    }
}