package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.BulletinPost;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulletinPostRepository extends JpaRepository<BulletinPost, UUID> {

    List<BulletinPost> findByAuthorId(UUID authorId);

    boolean existsByAuthorId(UUID authorId);

    List<BulletinPost> findByCategory(String category);

    List<BulletinPost> findAllByOrderByCreatedAtDesc();

    List<BulletinPost> findByStudentGroupIdOrderByCreatedAtDesc(UUID studentGroupId);

    List<BulletinPost> findByStudentGroupIsNullOrderByCreatedAtDesc();

    List<BulletinPost> findByCategoryOrderByCreatedAtDesc(String category);
}