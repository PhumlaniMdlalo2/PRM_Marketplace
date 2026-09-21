package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BulletinPostServiceImpl implements IBulletinPostService {

    private final BulletinPostRepository bulletinPostRepository;

    public BulletinPostServiceImpl(BulletinPostRepository bulletinPostRepository) {
        this.bulletinPostRepository = bulletinPostRepository;
    }

    @Override
    public BulletinPost create(BulletinPost bulletinPost) {
        if (bulletinPost == null) {
            return null;
        }
        return bulletinPostRepository.save(bulletinPost);
    }

    @Override
    public BulletinPost read(UUID id) {
        if (id == null) {
            return null;
        }
        return bulletinPostRepository.findById(id).orElse(null);
    }

    @Override
    public BulletinPost update(BulletinPost bulletinPost) {
        if (bulletinPost == null || bulletinPost.getId() == null
                || !bulletinPostRepository.existsById(bulletinPost.getId())) {
            return null;
        }
        return bulletinPostRepository.save(bulletinPost);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !bulletinPostRepository.existsById(id)) {
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
    public Optional<BulletinPost> findByAuthorId(UUID authorId) {
        List<BulletinPost> posts = bulletinPostRepository.findByAuthorId(authorId);
        return posts.isEmpty() ? Optional.empty() : Optional.of(posts.get(0));
    }
}