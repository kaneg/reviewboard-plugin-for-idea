/**
 * $Source:$
 * $Id:$
 *
 * Copyright 2008 KBC Financial Products - Risk Technology
 */
package rb;

/**
 * Encodes and decodes passwords
 *
 * @author <a href="mailto:Chris.Miller@kbcfp.com">Chris Miller</a>
 * @version $Revision: 1.0$
 */
public class PasswordMangler {
    private static final Encrypter ENCRYPTER = new Encrypter("*&^$^%3&8,.@;as!=-#/!21".toCharArray());

    static String encode(String password) {
        if (password == null) {
            return null;
        }
        return ENCRYPTER.encrypt(password);
    }

    static String decode(String encodedPassword) {
        if (encodedPassword == null) {
            return null;
        }
        return ENCRYPTER.decrypt(encodedPassword);
    }
}
