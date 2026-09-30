package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.MessageRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements IConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public Conversation create(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        return conversationRepository.save(conversation);
    }

    @Override
    public Conversation read(UUID id) {
        if (id == null) {
            return null;
        }
        return conversationRepository.findById(id).orElse(null);
    }

    @Override
    public Conversation update(Conversation conversation) {
        if (conversation == null || conversation.getId() == null
                || !conversationRepository.existsById(conversation.getId())) {
            return null;
        }
        return conversationRepository.save(conversation);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !conversationRepository.existsById(id)) {
            return false;
        }
        conversationRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Conversation> getAll() {
        return conversationRepository.findAll();
    }

    @Override
    public List<Conversation> getForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(userId, userId);
    }

    @Override
    public Conversation getOrCreate(User buyer, User seller, Product product) {
        if (buyer == null || seller == null) {
            return null;
        }
        if (buyer.getId() != null && buyer.getId().equals(seller.getId())) {
            return null;
        }

        UUID productId = product == null ? null : product.getId();
        return findExisting(buyer.getId(), seller.getId(), productId)
                .orElseGet(() -> conversationRepository.save(new Conversation.Builder()
                        .setBuyer(buyer)
                        .setSeller(seller)
                        .setProduct(product)
                        .build()));
    }

    private java.util.Optional<Conversation> findExisting(UUID buyerId, UUID sellerId, UUID productId) {
        if (productId == null) {
            return conversationRepository.findByBuyerIdAndSellerIdAndProductIsNull(buyerId, sellerId);
        }
        return conversationRepository.findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId);
    }

    @Override
    public Conversation readForParticipant(UUID id, UUID userId) {
        Conversation conversation = read(id);
        if (conversation == null) {
            return null;
        }
        boolean isBuyer = conversation.getBuyer() != null
                && conversation.getBuyer().getId() != null
                && conversation.getBuyer().getId().equals(userId);
        boolean isSeller = conversation.getSeller() != null
                && conversation.getSeller().getId() != null
                && conversation.getSeller().getId().equals(userId);
        return (isBuyer || isSeller) ? conversation : null;
    }

    @Override
    public long unreadCount(UUID userId) {
        if (userId == null) {
            return 0L;
        }
        return conversationRepository.countByBuyerIdOrSellerId(userId, userId);
    }
}