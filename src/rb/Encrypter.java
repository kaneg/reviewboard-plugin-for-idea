/**
 * $Source:$
 * $Id:$
 */
package rb;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Encrypts/Decrypts strings using a caller-supplied passphrase.
 *
 * @author <a href="mailto:chris_overseas@hotmail.com">Chris Miller</a>
 * @version $Revision: 1.0$
 */
public class Encrypter {
  private char[] key;
  private PBEParameterSpec paramSpec = new PBEParameterSpec(
      new byte[]{
          (byte) 0xc4, (byte) 0x12, (byte) 0x5e, (byte) 0x0c,
          (byte) 0x6e, (byte) 0xff, (byte) 0xce, (byte) 0x2a
      },
      23
  );

  /**
   * Constructs an encrypter that uses the given characters as an encryption key.
   */
  public Encrypter(char[] key) {
    this.key = key;
  }

  /**
   * Returns an encrypted, base64 encoded version of the supplied string.
   */
  public String encrypt(String data) {
    try {
      PBEKeySpec keySpec = new PBEKeySpec(key);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = factory.generateSecret(keySpec);

      Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
      cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

      byte[] inputBytes = data.getBytes("UTF8");
      byte[] outputBytes = cipher.doFinal(inputBytes);

      BASE64Encoder encoder = new BASE64Encoder();
      return encoder.encode(outputBytes);
    } catch (Exception e) {
      throw new EncrypterException("Failed to encrypt string.", e);
    }
  }

  /**
   * Returns the unencrypted contents of the supplied encrypted, base64 encoded string.
   */
  public String decrypt(String encryptedData) {
    try {
      BASE64Decoder decoder = new BASE64Decoder();
      byte[] inputBytes = decoder.decodeBuffer(encryptedData);

      PBEKeySpec keySpec = new PBEKeySpec(key);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = factory.generateSecret(keySpec);

      Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
      cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

      byte[] outputBytes = cipher.doFinal(inputBytes);
      return new String(outputBytes, "UTF8");
    } catch (Exception e) {
      throw new EncrypterException("Failed to decrypt string.", e);
    }
  }

  public static class EncrypterException extends RuntimeException {
    public EncrypterException(String message) {
      super(message);
    }

    public EncrypterException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
