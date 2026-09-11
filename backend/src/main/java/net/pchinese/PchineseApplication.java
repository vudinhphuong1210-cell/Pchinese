package net.pchinese;

import net.pchinese.security.PchineseSecurityProperties;
import net.pchinese.auth.application.PchineseMailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({ PchineseSecurityProperties.class, PchineseMailProperties.class })
public class PchineseApplication {
    public static void main(String[] args) {
        SpringApplication.run(PchineseApplication.class, args);
    }
}
