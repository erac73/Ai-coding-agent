package com.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommandValidatorTest {

    private final CommandValidator validator = new CommandValidator(false);

    @Test
    void shouldAllowSafeCommands() {
        assertThat(validator.validate("ls -la").isAllowed()).isTrue();
        assertThat(validator.validate("git status").isAllowed()).isTrue();
        assertThat(validator.validate("mvn test").isAllowed()).isTrue();
        assertThat(validator.validate("echo hello").isAllowed()).isTrue();
    }

    @Test
    void shouldBlockRmRfRoot() {
        assertThat(validator.validate("rm -rf /").isAllowed()).isFalse();
        assertThat(validator.validate("sudo rm -rf /var").isAllowed()).isFalse();
    }

    @Test
    void shouldBlockShutdown() {
        assertThat(validator.validate("shutdown -h now").isAllowed()).isFalse();
        assertThat(validator.validate("reboot").isAllowed()).isFalse();
    }

    @Test
    void shouldBlockMkfs() {
        assertThat(validator.validate("mkfs.ext4 /dev/sda1").isAllowed()).isFalse();
    }

    @Test
    void shouldBlockSqlDestructive() {
        assertThat(validator.validate("DROP DATABASE mydb").isAllowed()).isFalse();
    }

    @Test
    void shouldBlockEmptyCommand() {
        assertThat(validator.validate("").isAllowed()).isFalse();
        assertThat(validator.validate(null).isAllowed()).isFalse();
    }

    @Test
    void shouldBlockWarnCommandsInStrictMode() {
        CommandValidator strict = new CommandValidator(true);
        assertThat(strict.validate("rm -rf temp/").isAllowed()).isFalse();
        assertThat(strict.validate("git push --force origin main").isAllowed()).isFalse();
        assertThat(strict.validate("git reset --hard HEAD").isAllowed()).isFalse();
    }

    @Test
    void shouldAllowWarnCommandsInNormalMode() {
        assertThat(validator.validate("rm -rf temp/").isAllowed()).isTrue();
    }
}
