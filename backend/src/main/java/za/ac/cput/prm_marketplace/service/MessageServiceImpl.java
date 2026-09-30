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
    public Message create(Message message) {
        if (message == null) {
            return null;
        }
        Message saved = messageRepository.save(message);
        touchConversation(message);
        return saved;
    }

    @Override
    public Message read(UUID id) {
        if (id == null) {
            return null;
        }
        return messageRepository.findById(id).orElse(null);
    }

    @Override
    public Message update(Message message) {
        if (message == null || message.getId() == null || !messageRepository.existsById(message.getId())) {
            return null;
        }
        return messageRepository.save(message);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !messageRepository.existsById(id)) {
            return false;
        }
        messageRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Message> getAll() {
        return messageRepository.findAll();
    }

    @Override
    public List<Message> getByConversation(UUID conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Override
    @Transactional
    public Message send(UUID conversationId, UUID senderId, String body) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || senderId == null || body == null || body.isBlank()) {
            return null;
        }
        if (!isParticipant(conversation, senderId)) {
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

    private boolean isParticipant(Conversation conversation, UUID userId) {
        boolean isBuyer = conversation.getBuyer() != null
                && conversation.getBuyer().getId() != null
                && conversation.getBuyer().getId().equals(userId);
        boolean isSeller = conversation.getSeller() != null
                && conversation.getSeller().getId() != null
                && conversation.getSeller().getId().equals(userId);
        return isBuyer || isSeller;
    }

    @Override
    @Transactional
    public int markRead(UUID conversationId, UUID readerId) {
        if (conversationId == null || readerId == null) {
            return 0;
        }
        return messageRepository.markConversationRead(conversationId, readerId, MessageStatus.READ);
    }

    @Override
    public long unreadCount(UUID conversationId) {
        if (conversationId == null) {
            return 0L;
        }
        return messageRepository.countByConversationIdAndStatusNot(conversationId, MessageStatus.READ);
    }

    private void touchConversation(Message message) {
        if (message == null || message.getConversation() == null) {
            return;
        }
        Conversation conversation = message.getConversation();
        conversation.touch();
        conversationRepository.save(conversation);
    }
}