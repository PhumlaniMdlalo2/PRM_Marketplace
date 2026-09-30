package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;

import java.util.List;
import java.util.UUID;

public interface IConversationService {

    Conversation create(Conversation conversation);

    Conversation read(UUID id);

    Conversation update(Conversation conversation);

    boolean delete(UUID id);

    List<Conversation> getAll();

    List<Conversation> getForUser(UUID userId);

    Conversation getOrCreate(User buyer, User seller, Product product);

    Conversation readForParticipant(UUID id, UUID userId);

    long unreadCount(UUID userId);
}