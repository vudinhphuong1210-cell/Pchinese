package net.pchinese.security;

import java.util.Set;
import java.util.UUID;

public record UserPrincipal(UUID userId, UUID sessionId, long authzVersion, Set<String> roles) {
    public boolean isAdmin() { return roles.contains("ADMIN"); }
}
