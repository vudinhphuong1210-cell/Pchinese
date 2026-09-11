package net.pchinese.review.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SrsReviewEventRepository extends JpaRepository<SrsReviewEventEntity, UUID> {

    Optional<SrsReviewEventEntity> findByUserIdAndClientReviewId(UUID userId, UUID clientReviewId);
}
