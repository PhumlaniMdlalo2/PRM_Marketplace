package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.BulletinPost;

import java.util.List;
import java.util.UUID;

/**
 * Reading a board post is public. Writing is not: the author is taken from the token, and editing
 * or removing a post requires being its author.
 */
public interface IBulletinPostService {

    /** Creates a post owned by the caller. */
    BulletinPost create(BulletinPost bulletinPost, UUID authorId);

    BulletinPost read(UUID id);

    /** @return the updated post, or null when it does not exist or the caller is not the author */
    BulletinPost update(BulletinPost bulletinPost, UUID requesterId);

    /** @return false when it does not exist or the caller is not the author */
    boolean delete(UUID id, UUID requesterId);

    List<BulletinPost> getAll();

    List<BulletinPost> getByAuthor(UUID authorId);
}