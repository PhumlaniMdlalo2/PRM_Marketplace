package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Comment;

import java.util.List;
import java.util.UUID;

/**
 * Comments on the public board are readable by anyone. Writing takes the author from the token, and
 * editing or removing a comment requires being its author.
 */
public interface ICommentService {

    /** Creates a comment authored by the caller. */
    Comment create(Comment comment, UUID authorId);

    Comment read(UUID id);

    /** @return the updated comment, or null when it does not exist or the caller is not the author */
    Comment update(Comment comment, UUID requesterId);

    /** @return false when it does not exist or the caller is not the author */
    boolean delete(UUID id, UUID requesterId);

    List<Comment> getByPost(UUID postId);

    List<Comment> getTopLevelByPost(UUID postId);

    List<Comment> getReplies(UUID postId, UUID parentId);

    List<Comment> getByAuthor(UUID authorId);

    long countByPost(UUID postId);
}