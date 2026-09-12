package net.pchinese.users.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.pchinese.users.domain.UserStatus;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailLookupHash(String emailLookupHash);

    Optional<UserEntity> findFirstByEmailLookupHashIn(Collection<String> emailLookupHashes);

    Optional<UserEntity> findByUserIdAndStatusAndEmailVerifiedAtIsNotNull(UUID userId, UserStatus status);

    Page<AdminUserSummary> findAllBy(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.userId = :userId")
    Optional<UserEntity> findLockedById(@Param("userId") UUID userId);

    interface AdminUserSummary {
        UUID getUserId();
        UserStatus getStatus();
        byte[] getDisplayNameCiphertext();
    }
}
