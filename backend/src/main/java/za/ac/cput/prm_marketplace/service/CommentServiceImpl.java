package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.CommentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final BulletinPostRepository bulletinPostRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              BulletinPostRepository bulletinPostRepository) {
        this.commentRepository = commentRepository;
        this.bulletinPostRepository = bulletinPostRepository;
    }

    @Override
    public Comment create(Comment comment) {
        if (comment == null || comment.getPost() == null) {
            return null;
        }
        Comment saved = commentRepository.save(comment);
        syncCommentCount(saved.getPost(), 1);
        return saved;
    }

    @Override
    public Comment read(UUID id) {
        if (id == null) {
            return null;
        }
        return commentRepository.findById(id).orElse(null);
    }

    @Override
    public Comment update(Comment comment) {
        if (comment == null || comment.getId() == null || !commentRepository.existsById(comment.getId())) {
            return null;
        }
        return commentRepository.save(comment);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !commentRepository.existsById(id)) {
            return false;
        }
        Comment existing = commentRepository.findById(id).orElse(null);
        commentRepository.deleteById(id);
        if (existing != null) {
            syncCommentCount(existing.getPost(), -1);
        }
        return true;
    }

    @Override
    public List<Comment> getAll() {
        return commentRepository.findAll();
    }

    @Override
    public List<Comment> getByPost(UUID postId) {
        if (postId == null) {
            return List.of();
        }
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Override
    public List<Comment> getTopLevelByPost(UUID postId) {
        if (postId == null) {
            return List.of();
        }
        return commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId);
    }

    @Override
    public List<Comment> getReplies(UUID postId, UUID parentId) {
        if (postId == null || parentId == null) {
            return List.of();
        }
        return commentRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId, parentId);
    }

    @Override
    public List<Comment> getByAuthor(UUID authorId) {
        if (authorId == null) {
            return List.of();
        }
        return commentRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    @Override
    public long countByPost(UUID postId) {
        if (postId == null) {
            return 0L;
        }
        return commentRepository.countByPostId(postId);
    }

    private void syncCommentCount(BulletinPost post, int delta) {
        if (post == null || post.getId() == null) {
            return;
        }
        BulletinPost managed = bulletinPostRepository.findById(post.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        if (delta > 0) {
            managed.incrementCommentCount();
        } else {
            managed.decrementCommentCount();
        }
        bulletinPostRepository.save(managed);
    }
}