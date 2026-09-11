package net.pchinese.aibuddy.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pchinese.ai-buddy")
public class AiBuddyProperties {
    private String internalBaseUrl = "http://127.0.0.1:8081";
    private String internalHmacSecret = "";
    public String getInternalBaseUrl() { return internalBaseUrl; }
    public void setInternalBaseUrl(String internalBaseUrl) { this.internalBaseUrl = internalBaseUrl; }
    public String getInternalHmacSecret() { return internalHmacSecret; }
    public void setInternalHmacSecret(String internalHmacSecret) { this.internalHmacSecret = internalHmacSecret; }
}
