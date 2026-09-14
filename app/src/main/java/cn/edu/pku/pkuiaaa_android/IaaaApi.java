package cn.edu.pku.pkuiaaa_android;

import android.util.Log;
import cn.edu.pku.pkurunner.Utils.IaaaWrapper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;

abstract class IaaaApi {
    private static final String SIGNING_KEY = "7696baa1fa4ed9679441764a271e556e";

    private static String readResponse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream), "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }

    private static String md5Hex(InputStream inputStream) throws Exception {
        int read;
        int index;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] buffer = new byte[4096];
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        do {
            read = bufferedInputStream.read(buffer);
            if (read > 0) {
                messageDigest.update(buffer, 0, read);
            }
        } while (read != -1);
        bufferedInputStream.close();
        byte[] digest = messageDigest.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (index = 0; index < digest.length; index++) {
            int unsignedByte = digest[index] & 0xFF;
            if (unsignedByte < 16) stringBuffer.append("0");
            stringBuffer.append(Integer.toHexString(unsignedByte));
        }
        return stringBuffer.toString();
    }

    private static String md5Hex(String text) throws Exception {
        return md5Hex(new ByteArrayInputStream(text.getBytes()));
    }

    private static String signedRequest(String endpoint, TreeMap<String, String> params) throws Exception {
        StringBuilder form = new StringBuilder();
        for (String key : params.keySet()) {
            if (form.length() != 0) form.append('&');
            form.append(key).append('=').append(params.get(key));
        }
        String base = form.toString();
        String signed = base + "&msgAbs=" + md5Hex(base + SIGNING_KEY);
        byte[] bytes = signed.getBytes("UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        connection.getOutputStream().write(bytes);
        if (connection.getResponseCode() != 200) throw new Exception("服务器返回数据错误！");
        return readResponse(connection.getInputStream());
    }

    private static String signedLogin(String username, String password, String code, String appId, String mode) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("userName", username);
        params.put("password", password);
        params.put("randCode", "");
        params.put("appId", appId);
        params.put("smsCode", "SMS".equals(mode) ? code : "");
        params.put("otpCode", "SMS".equals(mode) ? "" : code);
        JSONObject response = new JSONObject(signedRequest(
                "https://iaaa.pku.edu.cn/iaaa/svc/authen/login.do", params));
        Log.d("iAAA", "login response: success=" + response.optString(IaaaWrapper.RESULT_SUCCESS));
        if (response.optBoolean(IaaaWrapper.RESULT_SUCCESS, false)) {
            String token = response.optString("token", "");
            if (!token.isEmpty()) return token;
        }
        throw new Exception("认证失败！" + response.optString("errMsg", ""));
    }

    private static String signedAuthMode(String username, String appId) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("userName", username);
        params.put("appId", appId);
        JSONObject response = new JSONObject(signedRequest(
                "https://iaaa.pku.edu.cn/iaaa/svc/authen/isMobileAuthen.do", params));
        Log.d("iAAA", "auth-mode response: success=" + response.optString(IaaaWrapper.RESULT_SUCCESS)
                + ", authenMode=" + response.optString("authenMode")
                + ", isBind=" + response.optString("isBind"));
        if (!response.optBoolean(IaaaWrapper.RESULT_SUCCESS, false)) {
            throw new Exception("查验验证选项失败！" + response.optString("errMsg", ""));
        }
        String mode = response.optString("authenMode", "");
        if ("SMS".equals(mode)) return "SMS";
        if ("OTP".equals(mode) && response.optBoolean("isBind", false)) return "OTP";
        return "";
    }

    static String requestSmsCode(String username, String appId) throws Exception {
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("userName", username);
        treeMap.put("appId", appId);
        StringBuilder sb = new StringBuilder();
        for (String key : treeMap.keySet()) {
            String value = treeMap.get(key);
            StringBuilder sb2 = sb.length() != 0 ? new StringBuilder("&") : new StringBuilder();
            sb2.append(key);
            sb2.append("=");
            sb2.append(value);
            sb.append(sb2.toString());
        }
        String body = sb.toString();
        try {
            String signature = md5Hex(body + SIGNING_KEY);
            if (signature == null) {
                throw new Exception("数据处理错误！");
            }
            String payload = body + "&msgAbs=" + signature;
            try {
                URL url = new URL("https://iaaa.pku.edu.cn/iaaa/svc/authen/sendSMSCode.do");
                byte[] bytes = payload.getBytes();
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                    connection.getOutputStream().write(bytes);
                    if (connection.getResponseCode() != 200) {
                        throw new Exception("服务器返回数据错误，发送验证码失败！");
                    }
                    InputStream inputStream = connection.getInputStream();
                    StringBuilder responseBuilder = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        responseBuilder.append(line);
                    }
                    JSONObject response = new JSONObject(responseBuilder.toString());
                    if (response.has(IaaaWrapper.RESULT_SUCCESS)) {
                        if (response.getString(IaaaWrapper.RESULT_SUCCESS).equals("true")) {
                            return response.has("mobileMask") ? response.getString("mobileMask") : "";
                        }
                        throw new Exception("发送验证码失败！");
                    }
                    if (!response.has("errMsg")) {
                        throw new Exception("发送验证码失败！");
                    }
                    throw new Exception("发送验证码失败！" + response.getString("errMsg"));
                } catch (IOException error) {
                    throw new Exception("服务器返回数据错误，发送验证码失败！");
                }
            } catch (MalformedURLException error) {
                throw new Exception("URL错误！");
            }
        } catch (Exception error) {
            Log.e("iAAA", "sendSMSCode request failed: " + error.getMessage(), error);
            throw error;
        }
    }

    static String login(String username, String password, String code, String appId, String mode) throws Exception {
        return signedLogin(username, password, code, appId, mode);
    }

    static String getAuthMode(String username, String appId) throws Exception {
        return signedAuthMode(username, appId);
    }
}
