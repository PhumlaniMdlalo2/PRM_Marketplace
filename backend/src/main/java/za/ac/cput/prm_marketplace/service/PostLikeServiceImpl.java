package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.PostLikeRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
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
    public PostLike create(PostLike postLike) {
        if (postLike == null) {
            return null;
        }
        return postLikeRepository.save(postLike);
    }

    @Override
    public PostLike read(UUID id) {
        if (id == null) {
            return null;
        }
        return postLikeRepository.findById(id).orElse(null);
    }

    @Override
    public PostLike update(PostLike postLike) {
        if (postLike == null || postLike.getId() == null || !postLikeRepository.existsById(postLike.getId())) {
            return null;
        }
        return postLikeRepository.save(postLike);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !postLikeRepository.existsById(id)) {
            return false;
        }
        postLikeRepository.deleteById(id);
        return true;
    }

    @Override
    public List<PostLike> getAll() {
        return postLikeRepository.findAll();
    }

    @Override
    public PostLike toggle(UUID postId, UUID userId) {
        if (postId == null || userId == null) {
            return null;
        }

        var existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            syncLikeCount(postId, -1);
            return null;
        }

        BulletinPost post = bulletinPostRepository.findById(postId).orElse(null);
        if (post == null) {
            return null;
        }

        User managedUser = userRepository.findById(userId).orElse(null);
        if (managedUser == null) {
            return null;
        }

        PostLike saved = postLikeRepository.save(new PostLike.Builder()
                .setPost(post)
                .setUser(managedUser)
                .build());
        syncLikeCount(postId, 1);
        return saved;
    }

    @Override
    public boolean hasLiked(UUID postId, UUID userId) {
        if (postId == null || userId == null) {
            return false;
        }
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public long countByPost(UUID postId) {
        if (postId == null) {
            return 0L;
        }
        return postLikeRepository.countByPostId(postId);
    }

    @Override
    public List<PostLike> getByUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return postLikeRepository.findByUserId(userId);
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