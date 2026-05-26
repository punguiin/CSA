import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class MessageCipher {
    private static final String alg = "AES";
    private final SecretKeySpec key;

    public MessageCipher(byte[] key){
        this.key = new SecretKeySpec(key, alg);
    }

    public byte[] encrypt(byte[] src) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(src);
    }

    public byte[] decrypt(byte[] src) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipher.doFinal(src);
    }
}
