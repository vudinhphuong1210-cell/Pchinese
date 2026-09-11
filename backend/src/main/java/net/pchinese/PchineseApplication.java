package net.pchinese;

import net.pchinese.security.PchineseSecurityProperties;
import net.pchinese.auth.application.PchineseMailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({ PchineseSecurityProperties.class, PchineseMailProperties.class })
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PchineseApplication {
    public static void main(String[] args) {
        SpringApplication.run(PchineseApplication.class, args);
    }
}
