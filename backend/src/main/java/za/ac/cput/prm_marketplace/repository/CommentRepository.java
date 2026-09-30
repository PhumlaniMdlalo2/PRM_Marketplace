package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Comment;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(UUID postId);

    List<Comment> findByPostIdAndParentIdOrderByCreatedAtAsc(UUID postId, UUID parentId);

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}