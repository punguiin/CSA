package Lab01;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageCipherTest {

    private static final byte[] KEY = "test_key_encrypt".getBytes();

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        byte[] plaintext = "Test text".getBytes();

        byte[] ciphertext = cipher.encrypt(plaintext);
        byte[] decrypted = cipher.decrypt(ciphertext);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void ciphertextDiffersFromPlaintext() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        byte[] plaintext = "secret".getBytes();

        byte[] ciphertext = cipher.encrypt(plaintext);

        assertFalse(java.util.Arrays.equals(plaintext, ciphertext));
    }

    @Test
    void ciphertextLengthIsBlockAligned() throws Exception {
        MessageCipher cipher = new MessageCipher(KEY);
        byte[] ciphertext = cipher.encrypt("hi".getBytes());

        assertEquals(0, ciphertext.length % 16);
    }

    @Test
    void wrongKeyFailsToDecrypt() throws Exception {
        MessageCipher encoder = new MessageCipher(KEY);
        MessageCipher decoder = new MessageCipher("WrongKey12345678".getBytes());
        byte[] ciphertext = encoder.encrypt("Test text".getBytes());

        assertThrows(Exception.class, () -> decoder.decrypt(ciphertext));
    }
}
