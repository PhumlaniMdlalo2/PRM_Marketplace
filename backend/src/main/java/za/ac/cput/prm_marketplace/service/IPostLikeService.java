package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.PostLike;

import java.util.List;
import java.util.UUID;

/**
 * A like is the caller's own action on a public post, so liking takes the user from the token. The
 * generic create, update and list-everything operations are gone.
 */
public interface IPostLikeService {

    /**
     * Likes the post as the caller, or removes the caller's like if one is already there.
     *
     * @return the new like, or null when the caller's like was removed or the post does not exist
     */
    PostLike toggle(UUID postId, UUID requesterId);

    /** Whether the caller has liked the post. */
    boolean hasLiked(UUID postId, UUID requesterId);

    long countByPost(UUID postId);

    /** The posts the caller has liked. */
    List<PostLike> getByUser(UUID requesterId);
}