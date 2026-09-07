package net.pchinese.auth.application;

import net.pchinese.common.error.ApiException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 12 || password.length() > 128
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)) {
            throw ApiException.validation("Password must be 12–128 characters and include upper-case, lower-case and numeric characters.");
        }
    }
}
