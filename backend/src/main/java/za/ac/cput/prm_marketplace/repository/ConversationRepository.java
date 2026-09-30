package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(UUID buyerId, UUID sellerId);

    List<Conversation> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, UUID productId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductIsNull(UUID buyerId, UUID sellerId);

    long countByBuyerIdOrSellerId(UUID buyerId, UUID sellerId);
}