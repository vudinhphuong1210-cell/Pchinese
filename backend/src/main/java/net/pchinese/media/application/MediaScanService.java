package net.pchinese.media.application;

import net.pchinese.content.api.ResourceNotFoundException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Entry point for the trusted scanner worker; it is intentionally not an ADMIN browser endpoint. */
@Service
public class MediaScanService {
    private static final Set<String> TERMINAL_SCAN_RESULTS = Set.of("CLEAN", "INFECTED", "FAILED");
    private final MediaAssetRepository mediaAssetRepository;

    public MediaScanService(MediaAssetRepository mediaAssetRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional
    public MediaAssetEntity recordScanResult(UUID mediaAssetId, Long expectedVersion, String result) {
        MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));
        if (!Objects.equals(media.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale media version while recording scan result.");
        }
        if (media.getApprovalStatus() != ApprovalStatus.PENDING_SCAN) {
            throw new StateConflictException("Only PENDING_SCAN media may receive a scan result.");
        }
        String normalized = result == null ? "" : result.trim().toUpperCase();
        if (!TERMINAL_SCAN_RESULTS.contains(normalized)) {
            throw new StateConflictException("Unsupported scanner result.");
        }
        media.setMalwareScanStatus(normalized);
        return mediaAssetRepository.save(media);
    }
}
