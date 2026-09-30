package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.VerificationCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findByUserIdAndCodeAndUsedFalse(UUID userId, String code);

    List<VerificationCode> findByUserIdAndUsedFalse(UUID userId);

    void deleteByUserIdAndUsedFalse(UUID userId);
}