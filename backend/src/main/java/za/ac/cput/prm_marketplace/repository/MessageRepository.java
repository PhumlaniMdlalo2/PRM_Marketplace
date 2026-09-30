package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.MessageStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    List<Message> findByConversationIdOrderBySentAtDesc(UUID conversationId);

    long countByConversationIdAndStatusNot(UUID conversationId, MessageStatus status);

    @Modifying
    @Query("update Message m set m.status = :status where m.conversation.id = :conversationId "
            + "and m.sender.id <> :senderId and m.status <> :status")
    int markConversationRead(@Param("conversationId") UUID conversationId,
                             @Param("senderId") UUID senderId,
                             @Param("status") MessageStatus status);
}