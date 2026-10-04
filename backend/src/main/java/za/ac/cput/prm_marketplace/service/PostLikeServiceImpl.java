package za.ac.cput.prm_marketplace.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.PostLikeRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostLikeServiceImpl implements IPostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final BulletinPostRepository bulletinPostRepository;
    private final UserRepository userRepository;

    public PostLikeServiceImpl(PostLikeRepository postLikeRepository,
                               BulletinPostRepository bulletinPostRepository,
                               UserRepository userRepository) {
        this.postLikeRepository = postLikeRepository;
        this.bulletinPostRepository = bulletinPostRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PostLike toggle(UUID postId, UUID requesterId) {
        if (postId == null || requesterId == null) {
            return null;
        }
        BulletinPost post = bulletinPostRepository.findById(postId).orElse(null);
        if (post == null) {
            return null;
        }
        User user = userRepository.findById(requesterId).orElse(null);
        if (user == null) {
            return null;
        }

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, requesterId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            postLikeRepository.flush();
            syncLikeCount(postId, -1);
            return null;
        }

        PostLike saved;
        try {
            saved = postLikeRepository.save(new PostLike.Builder()
                    .setPost(post)
                    .setUser(user)
                    .build());
            postLikeRepository.flush();
        } catch (DataIntegrityViolationException raceLost) {
            // Two clicks arrived together and the unique index on (post_id, user_id) let only one
            // through. The second must behave like a like that was already there rather than
            // raising a 500 or double-counting.
            postLikeRepository.findByPostIdAndUserId(postId, requesterId)
                    .ifPresent(duplicate -> {
                        postLikeRepository.delete(duplicate);
                        postLikeRepository.flush();
                        syncLikeCount(postId, -1);
                    });
            return null;
        }
        syncLikeCount(postId, 1);
        return saved;
    }

    @Override
    public boolean hasLiked(UUID postId, UUID requesterId) {
        if (postId == null || requesterId == null) {
            return false;
        }
        return postLikeRepository.existsByPostIdAndUserId(postId, requesterId);
    }

    @Override
    public long countByPost(UUID postId) {
        if (postId == null) {
            return 0L;
        }
        return postLikeRepository.countByPostId(postId);
    }

    @Override
    public List<PostLike> getByUser(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return postLikeRepository.findByUserId(requesterId);
    }

    private void syncLikeCount(UUID postId, int delta) {
        BulletinPost post = bulletinPostRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        if (delta > 0) {
            post.incrementLikeCount();
        } else {
            post.decrementLikeCount();
        }
        bulletinPostRepository.save(post);
    }
}