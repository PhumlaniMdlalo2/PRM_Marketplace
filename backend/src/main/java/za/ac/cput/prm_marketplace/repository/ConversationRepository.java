package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.MessageStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(UUID buyerId, UUID sellerId);

    List<Conversation> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, UUID productId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductIsNull(UUID buyerId, UUID sellerId);

    /**
     * Counts the conversations the user still has unread messages in. The user's own
     * outgoing messages are ignored: a message they sent is never unread for them.
     * Counted by distinct conversation so a thread with several unread messages
     * contributes a single unread conversation.
     */
    @Query("select count(distinct m.conversation) from Message m "
            + "where m.sender.id <> :userId and m.status <> :readStatus "
            + "and (m.conversation.buyer.id = :userId or m.conversation.seller.id = :userId)")
    long countUnreadConversations(@Param("userId") UUID userId,
                                  @Param("readStatus") MessageStatus readStatus);
}