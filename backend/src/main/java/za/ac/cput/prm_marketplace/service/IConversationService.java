package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.Product;

import java.util.List;
import java.util.UUID;

/**
 * A conversation is readable only by its two participants. Every method is scoped by the caller's
 * id, and the previous {@code read}, {@code update}, {@code delete} and {@code getAll} operations
 * are gone: they let any authenticated caller open, rewrite or delete a private thread.
 */
public interface IConversationService {

    /**
     * Finds the existing thread between two people about a product, or starts one. The caller must
     * be one of the two parties, so nobody can open a thread in someone else's name.
     *
     * @return the conversation, or null when the requester is not a participant
     */
    Conversation getOrCreate(UUID requesterId, UUID otherPartyId, UUID productId);

    /** @return the conversation, or null when it does not exist or the caller is not a participant */
    Conversation read(UUID id, UUID requesterId);

    /** @return false when it does not exist or the caller is not a participant */
    boolean delete(UUID id, UUID requesterId);

    List<Conversation> getForUser(UUID requesterId);

    /**
     * How many of the caller's conversations hold messages they have not read. Messages the caller
     * sent themselves are not counted as unread.
     */
    long unreadCount(UUID requesterId);

    /** The participants' ids, used by the messaging service to authorise a thread. */
    List<UUID> participantIds(UUID conversationId);
}