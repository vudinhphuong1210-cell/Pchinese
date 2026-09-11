package net.pchinese.content.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<TopicEntity, UUID> {
    Optional<TopicEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
