package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Comment;

import java.util.List;
import java.util.UUID;

public interface ICommentService {

    Comment create(Comment comment);

    Comment read(UUID id);

    Comment update(Comment comment);

    boolean delete(UUID id);

    List<Comment> getAll();

    List<Comment> getByPost(UUID postId);

    List<Comment> getTopLevelByPost(UUID postId);

    List<Comment> getReplies(UUID postId, UUID parentId);

    List<Comment> getByAuthor(UUID authorId);

    long countByPost(UUID postId);
}