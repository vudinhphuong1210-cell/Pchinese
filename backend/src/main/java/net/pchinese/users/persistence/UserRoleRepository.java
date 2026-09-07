package net.pchinese.users.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.users.domain.RoleCode;
import net.pchinese.users.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
    @Query("select r.roleCode from UserRoleEntity r where r.user.userId = :userId and r.revokedAt is null")
    Set<RoleCode> findActiveRolesByUserId(@Param("userId") UUID userId);

    default Set<String> findActiveRoleCodesByUserId(UUID userId) {
        return findActiveRolesByUserId(userId).stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from UserRoleEntity r where r.user.userId = :userId and r.roleCode = :role and r.revokedAt is null")
    List<UserRoleEntity> findActiveLocked(@Param("userId") UUID userId, @Param("role") RoleCode role);

    @Query("select count(r) from UserRoleEntity r join r.user u where r.roleCode = :role and r.revokedAt is null and u.status = :status")
    long countActiveRolesForStatus(@Param("role") RoleCode role, @Param("status") UserStatus status);
}
