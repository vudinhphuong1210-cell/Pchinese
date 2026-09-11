package net.pchinese.security.crypto;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserDataKeyRepository extends JpaRepository<UserDataKeyEntity, UUID> { }
