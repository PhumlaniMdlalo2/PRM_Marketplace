package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.MessageStatus;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ConversationRepository;
import za.ac.cput.prm_marketplace.repository.MessageRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements IMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Message> getByConversation(UUID conversationId, UUID requesterId) {
        if (!isParticipant(conversationId, requesterId)) {
            return List.of();
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Override
    public Message read(UUID id, UUID requesterId) {
        if (id == null) {
            return null;
        }
        Message message = messageRepository.findById(id).orElse(null);
        if (message == null || message.getConversation() == null) {
            return null;
        }
        // Authorisation follows the message's own thread, so guessing an id cannot reach a
        // conversation the caller has no part in.
        return isParticipant(message.getConversation().getId(), requesterId) ? message : null;
    }

    @Override
    @Transactional
    public Message send(UUID conversationId, UUID senderId, String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !isParticipant(conversation, senderId)) {
            return null;
        }
        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null) {
            return null;
        }

        conversation.touch();
        conversationRepository.save(conversation);

        return messageRepository.save(new Message.Builder()
                .setConversation(conversation)
                .setSender(sender)
                .setBody(body.trim())
                .setStatus(MessageStatus.SENT)
                .build());
    }

    @Override
    @Transactional
    public int markRead(UUID conversationId, UUID readerId) {
        if (!isParticipant(conversationId, readerId)) {
            return 0;
        }
        // The update excludes the reader's own messages, so this cannot mark somebody else's
        // messages as read and cannot be used to disturb another participant's badge count.
        return messageRepository.markConversationRead(conversationId, readerId, MessageStatus.READ);
    }

    @Override
    public long unreadCount(UUID conversationId, UUID requesterId) {
        if (!isParticipant(conversationId, requesterId)) {
            return 0L;
        }
        return messageRepository.countByConversationIdAndStatusNotAndSender_IdNot(
                conversationId, MessageStatus.READ, requesterId);
    }

    private boolean isParticipant(UUID conversationId, UUID userId) {
        if (conversationId == null || userId == null) {
            return false;
        }
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        return isParticipant(conversation, userId);
    }

    private boolean isParticipant(Conversation conversation, UUID userId) {
        if (conversation == null || userId == null) {
            return false;
        }
        return matches(conversation.getBuyer(), userId) || matches(conversation.getSeller(), userId);
    }

    private boolean matches(User party, UUID userId) {
        return party != null && party.getId() != null && party.getId().equals(userId);
    }
}