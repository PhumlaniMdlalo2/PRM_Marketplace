package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.CommentRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

@Service
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final BulletinPostRepository bulletinPostRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              BulletinPostRepository bulletinPostRepository,
                              UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.bulletinPostRepository = bulletinPostRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Comment create(Comment comment, UUID authorId) {
        if (comment == null || comment.getPost() == null || authorId == null) {
            return null;
        }
        User author = userRepository.findById(authorId).orElse(null);
        if (author == null) {
            return null;
        }
        // Re-read the post rather than trusting the nested object in the body, so a comment cannot
        // be filed against a post that does not exist.
        BulletinPost post = bulletinPostRepository.findById(comment.getPost().getId()).orElse(null);
        if (post == null) {
            return null;
        }
        // A reply must point at a comment that exists and sits on the same post, otherwise the
        // thread could be wired to another post's comment.
        Comment parent = null;
        if (comment.getParent() != null) {
            parent = commentRepository.findById(comment.getParent().getId()).orElse(null);
            if (parent == null || !samePost(parent, post)) {
                return null;
            }
        }

        Comment created = new Comment.Builder()
                .setPost(post)
                .setAuthor(author)
                .setParent(parent)
                .setBody(comment.getBody())
                .build();
        Comment saved = commentRepository.save(created);
        syncCommentCount(post, 1);
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
    @Transactional
    public Comment update(Comment comment, UUID requesterId) {
        if (comment == null || comment.getId() == null) {
            return null;
        }
        Comment existing = commentRepository.findById(comment.getId()).orElse(null);
        if (existing == null || !isAuthor(existing, requesterId)) {
            return null;
        }
        // Only the body may change. The post, the author, the parent link and the timestamp are
        // carried over from the stored row so a body cannot re-parent or re-own a comment.
        Comment updated = new Comment.Builder()
                .copy(existing)
                .setBody(comment.getBody())
                .build();
        return commentRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (id == null) {
            return false;
        }
        Comment existing = commentRepository.findById(id).orElse(null);
        if (existing == null || !isAuthor(existing, requesterId)) {
            return false;
        }

        // fk_comments_parent has no ON DELETE action, so a comment that has replies cannot be
        // removed on its own. Drop the whole reply subtree first, deepest replies first.
        int removed = deleteReplySubtree(existing) + 1;
        commentRepository.delete(existing);

        syncCommentCount(existing.getPost(), -removed);
        return true;
    }

    /**
     * Deletes every descendant of {@code root}, deepest first, and returns how many were removed.
     * Walks the tree iteratively so that an unexpectedly deep reply chain cannot blow the stack.
     */
    private int deleteReplySubtree(Comment root) {
        List<Comment> collected = new ArrayList<>();
        Deque<UUID> pending = new ArrayDeque<>();
        pending.push(root.getId());

        while (!pending.isEmpty()) {
            UUID parentId = pending.pop();
            for (Comment reply : commentRepository.findByParentId(parentId)) {
                collected.add(reply);
                if (reply.getId() != null) {
                    pending.push(reply.getId());
                }
            }
        }

        // Replies were collected parent-first, so reverse order guarantees children go before parents.
        for (int i = collected.size() - 1; i >= 0; i--) {
            commentRepository.delete(collected.get(i));
        }
        return collected.size();
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

    private boolean isAuthor(Comment comment, UUID userId) {
        return comment.getAuthor() != null
                && comment.getAuthor().getId() != null
                && comment.getAuthor().getId().equals(userId);
    }

    private boolean samePost(Comment comment, BulletinPost post) {
        return comment.getPost() != null
                && comment.getPost().getId() != null
                && comment.getPost().getId().equals(post.getId());
    }

    private void syncCommentCount(BulletinPost post, int delta) {
        if (post == null || post.getId() == null || delta == 0) {
            return;
        }
        BulletinPost managed = bulletinPostRepository.findById(post.getId()).orElse(null);
        if (managed == null) {
            return;
        }
        if (delta > 0) {
            managed.incrementCommentCount();
        } else {
            managed.decrementCommentCount(-delta);
        }
        bulletinPostRepository.save(managed);
    }
}