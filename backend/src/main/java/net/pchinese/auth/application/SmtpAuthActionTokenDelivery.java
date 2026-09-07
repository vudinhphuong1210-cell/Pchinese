package net.pchinese.auth.application;

import net.pchinese.auth.domain.ActionTokenPurpose;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.users.persistence.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/** Sends one-time identity credentials without logging, persisting, or returning their raw values. */
@Component
@ConditionalOnProperty(prefix = "pchinese.mail", name = "enabled", havingValue = "true")
public class SmtpAuthActionTokenDelivery implements AuthActionTokenDelivery {
    private final JavaMailSender mailSender;
    private final UserRepository users;
    private final SensitiveValueService sensitiveValues;
    private final PchineseMailProperties properties;

    public SmtpAuthActionTokenDelivery(JavaMailSender mailSender, UserRepository users,
                                       SensitiveValueService sensitiveValues, PchineseMailProperties properties) {
        this.mailSender = mailSender;
        this.users = users;
        this.sensitiveValues = sensitiveValues;
        this.properties = properties;
    }

    @Override
    public void deliver(UUID userId, ActionTokenPurpose purpose, String rawToken) {
        String recipient = users.findById(userId)
                .map(user -> sensitiveValues.decryptToString(user.getEmailCiphertext()))
                .orElseThrow(ApiException::notFound);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(required(properties.getFrom(), "pchinese.mail.from"));
        message.setTo(recipient);
        message.setSubject(purpose == ActionTokenPurpose.EMAIL_VERIFICATION
                ? "Xác minh tài khoản Pchinese" : "Đặt lại mật khẩu Pchinese");
        message.setText(emailBody(purpose, actionUrl(purpose, rawToken)));
        mailSender.send(message);
    }

    private String actionUrl(ActionTokenPurpose purpose, String rawToken) {
        return UriComponentsBuilder.fromUriString(required(properties.getFrontendUrl(), "pchinese.mail.frontend-url"))
                .queryParam("auth", purpose == ActionTokenPurpose.EMAIL_VERIFICATION ? "verify" : "reset")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }
    private String emailBody(ActionTokenPurpose purpose, String url) {
        String action = purpose == ActionTokenPurpose.EMAIL_VERIFICATION ? "xác minh tài khoản" : "đặt lại mật khẩu";
        return "Bạn đã yêu cầu " + action + ".\n\n"
                + "Mở liên kết này để tiếp tục (có hiệu lực trong 1 giờ):\n" + url + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.";
    }
    private String required(String value, String key) {
        if (value == null || value.isBlank()) throw new IllegalStateException(key + " must be configured when email delivery is enabled.");
        return value;
    }
}
