package com.zufar.icedlatte.email.api.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatternReplacer unit tests")
class PatternReplacerTest {

    @Test
    @DisplayName("Replaces all hook characters in order")
    void replace_allHooks_producesExpectedString() {
        PatternReplacer replacer = new PatternReplacer("###", '#');
        replacer.replace('1');
        replacer.replace('2');
        replacer.replace('3');
        assertThat(replacer.toString()).isEqualTo("123");
    }

    @Test
    @DisplayName("isReplaceable returns true when hooks remain")
    void isReplaceable_withRemainingHooks_returnsTrue() {
        PatternReplacer replacer = new PatternReplacer("##", '#');
        assertThat(replacer.isReplaceable()).isTrue();
    }

    @Test
    @DisplayName("isReplaceable returns false after all hooks replaced")
    void isReplaceable_afterAllReplaced_returnsFalse() {
        PatternReplacer replacer = new PatternReplacer("#", '#');
        replacer.replace('5');
        assertThat(replacer.isReplaceable()).isFalse();
    }

    @Test
    @DisplayName("Pattern with no hooks is immediately not replaceable")
    void isReplaceable_noHooks_returnsFalse() {
        PatternReplacer replacer = new PatternReplacer("ABC", '#');
        assertThat(replacer.isReplaceable()).isFalse();
        assertThat(replacer.toString()).isEqualTo("ABC");
    }

    @Test
    @DisplayName("Mixed pattern preserves non-hook characters")
    void replace_mixedPattern_preservesStaticChars() {
        PatternReplacer replacer = new PatternReplacer("A#B#C", '#');
        replacer.replace('1');
        replacer.replace('2');
        assertThat(replacer.toString()).isEqualTo("A1B2C");
    }
}
