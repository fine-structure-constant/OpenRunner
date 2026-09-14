package cn.edu.pku.pkurunner.Utils;

import android.util.Base64;
import com.bumptech.glide.load.Key;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import kotlin.UByte;

public class SecUtil {

    private static class AesCipher {

        private static class Base64Codec {
            public static byte[] decode(String text) {
                return Base64.decode(text, 2);
            }

            public static byte[] encode(byte[] data) {
                return Base64.encode(data, 2);
            }
        }

        public static String decrypt(String cipherText, String key) {
            if (cipherText == null || cipherText.length() == 0) {
                return null;
            }
            byte[] encrypted = Base64Codec.decode(cipherText);
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(2, secretKeySpec);
                return new String(cipher.doFinal(encrypted), Key.STRING_CHARSET_NAME);
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }

        public static String encrypt(String plainText, String key) {
            if (plainText != null && plainText.length() != 0) {
                try {
                    byte[] bytes = plainText.getBytes(Key.STRING_CHARSET_NAME);
                    SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(1, secretKeySpec);
                    return new String(Base64Codec.encode(cipher.doFinal(bytes)));
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            return null;
        }
    }

    private static class Sha256 {
        private static String toHexString(byte[] data) {
            StringBuilder sb = new StringBuilder();
            for (byte current : data) {
                String hexString = Integer.toHexString(current & UByte.MAX_VALUE);
                if (hexString.length() == 1) {
                    sb.append("0");
                }
                sb.append(hexString);
            }
            return sb.toString();
        }

        public static String hash(String text) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(text.getBytes(Key.STRING_CHARSET_NAME));
                return toHexString(messageDigest.digest());
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e2) {
                e2.printStackTrace();
                return "";
            }
        }
    }

    public static String generateCheckField(String str, Date date) {
        return AesCipher.encrypt("android11", str + "_" + date.getTime());
    }

    public static String getAbstract(String str, String str2) {
        return Sha256.hash(str + '_' + str2 + "_YCVNc92y").substring(0, 32);
    }

    public static boolean verifyCheckField(String str, Date date, String str2) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("_");
        sb.append(date.getTime());
        return str2 != null && "android11".equals(AesCipher.decrypt(str2, sb.toString()));
    }
}
