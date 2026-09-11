package net.pchinese.shadowing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordingRepository extends JpaRepository<RecordingEntity, UUID> {
    Optional<RecordingEntity> findByRecordingIdAndUserId(UUID recordingId, UUID userId);
}
