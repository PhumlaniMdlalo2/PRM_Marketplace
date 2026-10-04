package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class BulletinPostServiceImpl implements IBulletinPostService {

    private final BulletinPostRepository bulletinPostRepository;
    private final UserRepository userRepository;

    public BulletinPostServiceImpl(BulletinPostRepository bulletinPostRepository,
                                   UserRepository userRepository) {
        this.bulletinPostRepository = bulletinPostRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BulletinPost create(BulletinPost bulletinPost, UUID authorId) {
        if (bulletinPost == null || authorId == null) {
            return null;
        }
        User author = userRepository.findById(authorId).orElse(null);
        if (author == null) {
            return null;
        }
        // The author comes from the token. A body naming another account is discarded, and the
        // id is cleared so a client cannot overwrite an existing post by supplying its id.
        BulletinPost created = new BulletinPost.Builder()
                .setTitle(bulletinPost.getTitle())
                .setBody(bulletinPost.getBody())
                .setCategory(bulletinPost.getCategory())
                .setImageUrl(bulletinPost.getImageUrl())
                .setAuthor(author)
                .build();
        return bulletinPostRepository.save(created);
    }

    @Override
    public BulletinPost read(UUID id) {
        if (id == null) {
            return null;
        }
        return bulletinPostRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public BulletinPost update(BulletinPost bulletinPost, UUID requesterId) {
        if (bulletinPost == null || bulletinPost.getId() == null) {
            return null;
        }
        BulletinPost existing = read(bulletinPost.getId());
        if (existing == null || !isAuthor(existing, requesterId)) {
            return null;
        }
        // Rebuild from the stored row: the author, the counters and the timestamp are not the
        // caller's to change, and the entity exposes no setters that could rewrite them anyway.
        BulletinPost updated = new BulletinPost.Builder()
                .copy(existing)
                .setTitle(bulletinPost.getTitle())
                .setBody(bulletinPost.getBody())
                .setCategory(bulletinPost.getCategory())
                .setImageUrl(bulletinPost.getImageUrl())
                .build();
        return bulletinPostRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        BulletinPost existing = read(id);
        if (existing == null || !isAuthor(existing, requesterId)) {
            return false;
        }
        bulletinPostRepository.deleteById(id);
        return true;
    }

    @Override
    public List<BulletinPost> getAll() {
        return bulletinPostRepository.findAll();
    }

    @Override
    public List<BulletinPost> getByAuthor(UUID authorId) {
        if (authorId == null) {
            return List.of();
        }
        return bulletinPostRepository.findByAuthorId(authorId);
    }

    private boolean isAuthor(BulletinPost post, UUID userId) {
        return post.getAuthor() != null
                && post.getAuthor().getId() != null
                && post.getAuthor().getId().equals(userId);
    }
}