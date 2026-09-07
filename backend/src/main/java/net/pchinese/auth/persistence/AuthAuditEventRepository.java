package net.pchinese.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEventEntity, UUID> { }
