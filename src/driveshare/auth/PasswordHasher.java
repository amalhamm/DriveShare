package driveshare.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Protects passwords and security-question answers.
 *
 * Nothing secret is stored in plain text. Each value is hashed with PBKDF2-HMAC-SHA256
 * (built into Java), using a random 16-byte salt per value and 65,536 iterations,
 * so two users with the same password still get different hashes.
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    /** Creates a new random salt (Base64 text so it can be stored in the database). */
    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes a secret with the given salt. */
    public static String hash(String secret, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), Base64.getDecoder().decode(salt),
                    ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Hashing is not available", e);
        }
    }

    /** Checks a typed secret against a stored hash (constant-time comparison). */
    public static boolean verify(String secret, String expectedHash, String salt) {
        byte[] actual = hash(secret, salt).getBytes();
        byte[] expected = expectedHash.getBytes();
        return MessageDigest.isEqual(actual, expected);
    }

    /** Answers are compared without caring about case or extra spaces ("Dearborn " = "dearborn"). */
    public static String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
