package com.zufar.icedlatte.common.validation.password;

public interface PasswordPolicy {
    void validateNewPassword(String password);
}
