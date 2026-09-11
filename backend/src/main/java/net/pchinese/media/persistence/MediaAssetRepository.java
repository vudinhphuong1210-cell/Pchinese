package net.pchinese.media.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {
    boolean existsByProviderNameAndProviderAssetIdentifier(String providerName, String providerAssetIdentifier);
}
