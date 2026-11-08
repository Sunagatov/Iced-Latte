package com.zufar.icedlatte.security.signup.password;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DefaultPasswordPolicyTest {
    private final com.zufar.icedlatte.common.validation.password.DefaultPasswordPolicy policy =
            new com.zufar.icedlatte.common.validation.password.DefaultPasswordPolicy();

    @Test
    void acceptsLongPassphrasesWithoutCompositionRules() {
        policy.validateNewPassword("gentle river coffee morning");
        policy.validateNewPassword("café lights over the river");
    }

    @Test
    void rejectsShortCommonAndRepeatedPasswords() {
        assertThatThrownBy(() -> policy.validateNewPassword("ShortPass1")).hasMessageContaining("15");
        assertThatThrownBy(() -> policy.validateNewPassword("passwordpassword")).hasMessageContaining("less common");
        assertThatThrownBy(() -> policy.validateNewPassword("x".repeat(40))).hasMessageContaining("less common");
    }
}
