package net.pchinese.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveValueServiceTest {
    @Test
    void emailLookupIsStableWhileLegacyAndTokenHashesRemainPepperScoped() {
        SensitiveValueService first = service("first-pepper");
        SensitiveValueService second = service("second-pepper");

        assertEquals(first.hashEmail("admin@pchinese.net"), second.hashEmail("admin@pchinese.net"));
        assertEquals("8a9b83999bd44e8c628a2185fbc01280ff3bdcc9d4553f314c965c1e2b399113",
                first.hashEmail("admin@pchinese.net"));
        assertNotEquals(first.legacyHashEmail("admin@pchinese.net"), second.legacyHashEmail("admin@pchinese.net"));
        assertNotEquals(first.hashToken("refresh-token"), second.hashToken("refresh-token"));
        assertTrue(first.emailLookupHashes("admin@pchinese.net").contains(first.hashEmail("admin@pchinese.net")));
        assertTrue(first.emailLookupHashes("admin@pchinese.net").contains(first.legacyHashEmail("admin@pchinese.net")));
    }

    private SensitiveValueService service(String pepper) {
        PchineseSecurityProperties properties = new PchineseSecurityProperties();
        properties.setTokenPepper(pepper);
        SensitiveValueService service = new SensitiveValueService(properties, new MockEnvironment());
        service.initialize();
        return service;
    }
}
