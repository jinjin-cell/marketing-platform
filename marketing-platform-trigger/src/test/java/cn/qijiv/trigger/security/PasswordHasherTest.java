package cn.qijiv.trigger.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordHasherTest {

    @Test
    public void hashesUseRandomSaltAndVerifyWithoutPlaintext() {
        PasswordHasher hasher = new PasswordHasher();
        PasswordHasher.PasswordHash first = hasher.hash("secure-password-123");
        PasswordHasher.PasswordHash second = hasher.hash("secure-password-123");

        assertNotEquals(first.getSalt(), second.getSalt());
        assertNotEquals(first.getHash(), second.getHash());
        assertTrue(hasher.matches("secure-password-123", first.getHash(), first.getSalt()));
        assertFalse(hasher.matches("wrong-password-123", first.getHash(), first.getSalt()));
    }
}
