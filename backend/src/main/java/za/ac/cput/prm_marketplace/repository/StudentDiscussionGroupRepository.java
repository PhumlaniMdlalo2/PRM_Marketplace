package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.prm_marketplace.domain.StudentDiscussionGroup;

import java.util.List;
import java.util.UUID;

public interface StudentDiscussionGroupRepository extends JpaRepository<StudentDiscussionGroup, UUID> {
    List<StudentDiscussionGroup> findByCampusIgnoreCaseOrderByNameAsc(String campus);
    boolean existsByCampusIgnoreCaseAndNameIgnoreCase(String campus, String name);
}
