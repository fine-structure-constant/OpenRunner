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

    private static class a {

        /* renamed from: cn.edu.pku.pkurunner.Utils.SecUtil$a$a, reason: collision with other inner class name */
        private static class C0040a {
            public static byte[] a(String str) {
                return Base64.decode(str, 2);
            }

            public static byte[] b(byte[] bArr) {
                return Base64.encode(bArr, 2);
            }
        }

        public static String a(String str, String str2) {
            if (str == null || str.length() == 0) {
                return null;
            }
            byte[] a2 = C0040a.a(str);
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(str2.getBytes(), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(2, secretKeySpec);
                return new String(cipher.doFinal(a2), Key.STRING_CHARSET_NAME);
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }

        public static String b(String str, String str2) {
            if (str != null && str.length() != 0) {
                try {
                    byte[] bytes = str.getBytes(Key.STRING_CHARSET_NAME);
                    SecretKeySpec secretKeySpec = new SecretKeySpec(str2.getBytes(), "AES");
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(1, secretKeySpec);
                    return new String(C0040a.b(cipher.doFinal(bytes)));
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            return null;
        }
    }

    private static class b {
        private static String a(byte[] bArr) {
            StringBuilder sb = new StringBuilder();
            for (byte b2 : bArr) {
                String hexString = Integer.toHexString(b2 & UByte.MAX_VALUE);
                if (hexString.length() == 1) {
                    sb.append("0");
                }
                sb.append(hexString);
            }
            return sb.toString();
        }

        public static String b(String str) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(str.getBytes(Key.STRING_CHARSET_NAME));
                return a(messageDigest.digest());
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e2) {
                e2.printStackTrace();
                return "";
            }
        }
    }

    public static String generateCheckField(String str, Date date) {
        return a.b("android11", str + "_" + date.getTime());
    }

    public static String getAbstract(String str, String str2) {
        return b.b(str + '_' + str2 + "_YCVNc92y").substring(0, 32);
    }

    public static boolean verifyCheckField(String str, Date date, String str2) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("_");
        sb.append(date.getTime());
        return str2 != null && "android11".equals(a.a(str2, sb.toString()));
    }
}
