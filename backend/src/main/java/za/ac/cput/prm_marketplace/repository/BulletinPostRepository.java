package za.ac.cput.prm_marketplace.repository;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BulletinPostRepository extends JpaRepository<BulletinPost, UUID> {

    List<BulletinPost> findByAuthorId(UUID authorId);

    boolean existsByAuthorId(UUID authorId);
}