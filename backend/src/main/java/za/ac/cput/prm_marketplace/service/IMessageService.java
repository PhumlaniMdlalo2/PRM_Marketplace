package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Message;

import java.util.List;
import java.util.UUID;

public interface IMessageService {

    Message create(Message message);

    Message read(UUID id);

    Message update(Message message);

    boolean delete(UUID id);

    List<Message> getAll();

    List<Message> getByConversation(UUID conversationId);

    Message send(UUID conversationId, UUID senderId, String body);

    int markRead(UUID conversationId, UUID readerId);

    long unreadCount(UUID conversationId);
}