package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Message;

import java.util.List;
import java.util.UUID;

/**
 * Messages belong to a conversation and are visible only to that conversation's two participants.
 * Sending takes the sender from the token, never from the request, and the generic create, update
 * and list-everything operations are gone.
 */
public interface IMessageService {

    /** @return the messages, or an empty list when the caller is not a participant */
    List<Message> getByConversation(UUID conversationId, UUID requesterId);

    /**
     * Sends a message as the caller into a conversation they belong to.
     *
     * @return the sent message, or null when the thread does not exist, the caller is not a
     *         participant, or the body is empty
     */
    Message send(UUID conversationId, UUID senderId, String body);

    /**
     * Marks the caller's unread messages in the thread as read. Messages the caller sent are left
     * alone: a sender has by definition read their own message.
     *
     * @return the number of messages marked
     */
    int markRead(UUID conversationId, UUID readerId);

    /** Unread messages in the thread, excluding the caller's own. */
    long unreadCount(UUID conversationId, UUID requesterId);

    /** @return the message, or null when it does not exist or the caller is not a participant */
    Message read(UUID id, UUID requesterId);
}