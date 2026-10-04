package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Comment;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /*
     * Every association below is spelled with an underscore, e.g. Post_Id rather than PostId.
     *
     * That is not a style preference. Spring Data resolves a method name like findByParentId by
     * looking for a bean property called parentId first, and only traverses to parent.id if there is
     * no such property. Comment has a getParentId() for the response body, so the short form would
     * silently become a query against a non-existent attribute and fail at runtime with a 500 on the
     * delete path. The underscore says "walk to the association, then take its id", which no bean
     * property can intercept.
     */

    List<Comment> findByPost_IdOrderByCreatedAtAsc(UUID postId);

    List<Comment> findByPost_IdAndParentIsNullOrderByCreatedAtAsc(UUID postId);

    List<Comment> findByPost_IdAndParent_IdOrderByCreatedAtAsc(UUID postId, UUID parentId);

    List<Comment> findByParent_Id(UUID parentId);

    List<Comment> findByAuthor_IdOrderByCreatedAtDesc(UUID authorId);

    long countByPost_Id(UUID postId);

    void deleteByPost_Id(UUID postId);
}