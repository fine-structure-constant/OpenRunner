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
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;

abstract class a {
    private static String readResponse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream), "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }

    private static String oauthLogin(String username, String password, String code, String appId, String mode) throws Exception {
        String smsCode = "SMS".equals(mode) ? code : "";
        String otpCode = "OTP".equals(mode) ? code : "";
        String form = "appid=" + URLEncoder.encode(appId, "UTF-8")
                + "&userName=" + URLEncoder.encode(username, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8")
                + "&randCode="
                + "&smsCode=" + URLEncoder.encode(smsCode, "UTF-8")
                + "&otpCode=" + URLEncoder.encode(otpCode, "UTF-8")
                + "&redirUrl=" + URLEncoder.encode("http://elective.pku.edu.cn:80/elective2008/agent4Iaaa.jsp/../ssoLogin.do", "UTF-8");
        byte[] bytes = form.getBytes("UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL("https://iaaa.pku.edu.cn/iaaa/oauthlogin.do").openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        connection.getOutputStream().write(bytes);
        if (connection.getResponseCode() != 200) throw new Exception("服务器返回数据错误，认证失败！");
        JSONObject response = new JSONObject(readResponse(connection.getInputStream()));
        if (response.optBoolean(IaaaWrapper.RESULT_SUCCESS, false)) {
            String token = response.optString("token", "");
            if (token.length() != 0) return token;
        }
        JSONObject errors = response.optJSONObject("errors");
        String message = errors == null ? "认证失败！" : errors.optString("msg", "认证失败！");
        throw new Exception(message);
    }

    private static String queryAuthMode(String username, String appId) throws Exception {
        String query = "?userName=" + URLEncoder.encode(username, "UTF-8")
                + "&appId=" + URLEncoder.encode(appId, "UTF-8")
                + "&_rand=" + Double.toString(Math.random());
        HttpURLConnection connection = (HttpURLConnection) new URL("https://iaaa.pku.edu.cn/iaaa/isMobileAuthen.do" + query).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        if (connection.getResponseCode() != 200) throw new Exception("服务器返回数据错误，查验验证选项失败！");
        JSONObject response = new JSONObject(readResponse(connection.getInputStream()));
        if (!response.optBoolean(IaaaWrapper.RESULT_SUCCESS, false)) {
            throw new Exception("查验验证选项失败！" + response.optString("errMsg", ""));
        }
        String mode = response.optString("authenMode", "");
        if ("SMS".equals(mode)) return "SMS";
        if ("OTP".equals(mode) && response.optBoolean("isBind", false)) return "OTP";
        return "";
    }

    private static String a(InputStream inputStream) throws Exception {
        int read;
        int i2;
        byte b2;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] bArr = new byte[4096];
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        do {
            read = bufferedInputStream.read(bArr);
            if (read > 0) {
                messageDigest.update(bArr, 0, read);
            }
        } while (read != -1);
        bufferedInputStream.close();
        byte[] digest = messageDigest.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (i2 = 0; i2 < digest.length; i2++) {
            // Keep the byte unsigned. Using kotlin.UByte.MAX_VALUE here causes
            // sign extension when this decompiled Java is compiled again.
            int unsignedByte = digest[i2] & 0xFF;
            if (unsignedByte < 16) stringBuffer.append("0");
            stringBuffer.append(Integer.toHexString(unsignedByte));
        }
        return stringBuffer.toString();
    }

    private static String b(String str) throws Exception {
        return a(new ByteArrayInputStream(str.getBytes()));
    }

    private static String signedRequest(String endpoint, TreeMap<String, String> params) throws Exception {
        StringBuilder form = new StringBuilder();
        for (String key : params.keySet()) {
            if (form.length() != 0) form.append('&');
            form.append(key).append('=').append(params.get(key));
        }
        String base = form.toString();
        String signed = base + "&msgAbs=" + b(base + "7696baa1fa4ed9679441764a271e556e");
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

    static String c(String str, String str2) throws Exception {
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("userName", str);
        treeMap.put("appId", str2);
        StringBuilder sb = new StringBuilder();
        for (String str3 : treeMap.keySet()) {
            String str4 = (String) treeMap.get(str3);
            StringBuilder sb2 = sb.length() != 0 ? new StringBuilder("&") : new StringBuilder();
            sb2.append(str3);
            sb2.append("=");
            sb2.append(str4);
            sb.append(sb2.toString());
        }
        String sb3 = sb.toString();
        try {
            String b2 = b(sb3 + "7696baa1fa4ed9679441764a271e556e");
            if (b2 == null) {
                throw new Exception("数据处理错误！");
            }
            String str5 = sb3 + "&msgAbs=" + b2;
            try {
                URL url = new URL("https://iaaa.pku.edu.cn/iaaa/svc/authen/sendSMSCode.do");
                byte[] bytes = str5.getBytes();
                try {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(15000);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                    httpURLConnection.getOutputStream().write(bytes);
                    if (httpURLConnection.getResponseCode() != 200) {
                        throw new Exception("服务器返回数据错误，发送验证码失败！");
                    }
                    InputStream inputStream = httpURLConnection.getInputStream();
                    StringBuilder sb4 = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));
                    while (true) {
                        String readLine = bufferedReader.readLine();
                        if (readLine == null) {
                            break;
                        }
                        sb4.append(readLine);
                    }
                    JSONObject jSONObject = new JSONObject(sb4.toString());
                    if (jSONObject.has(IaaaWrapper.RESULT_SUCCESS)) {
                        if (jSONObject.getString(IaaaWrapper.RESULT_SUCCESS).equals("true")) {
                            return jSONObject.has("mobileMask") ? jSONObject.getString("mobileMask") : "";
                        }
                        throw new Exception("发送验证码失败！");
                    }
                    if (!jSONObject.has("errMsg")) {
                        throw new Exception("发送验证码失败！");
                    }
                    throw new Exception("发送验证码失败！" + jSONObject.getString("errMsg"));
                } catch (IOException unused2) {
                    throw new Exception("服务器返回数据错误，发送验证码失败！");
                }
            } catch (MalformedURLException unused3) {
                throw new Exception("URL错误！");
            }
        } catch (Exception error) {
            Log.e("iAAA", "sendSMSCode request failed: " + error.getMessage(), error);
            throw error;
        }
    }

    static String d(String str, String str2, String str3, String str4, String str5) throws Exception {
        return signedLogin(str, str2, str3, str4, str5);
        /*
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("userName", str);
        treeMap.put("password", str2);
        treeMap.put("randCode", "");
        treeMap.put("appId", str4);
        if (str5.equals("SMS")) {
            treeMap.put("smsCode", str3);
            treeMap.put("otpCode", "");
        } else {
            treeMap.put("smsCode", "");
            treeMap.put("otpCode", str3);
        }
        StringBuilder sb = new StringBuilder();
        for (String str6 : treeMap.keySet()) {
            String str7 = (String) treeMap.get(str6);
            StringBuilder sb2 = sb.length() != 0 ? new StringBuilder("&") : new StringBuilder();
            sb2.append(str6);
            sb2.append("=");
            sb2.append(str7);
            sb.append(sb2.toString());
        }
        String sb3 = sb.toString();
        try {
            String b2 = b(sb3 + "7696baa1fa4ed9679441764a271e556e");
            if (b2 == null) {
                throw new Exception("数据处理错误！");
            }
            String str8 = sb3 + "&msgAbs=" + b2;
            try {
                URL url = new URL("https://iaaa.pku.edu.cn/iaaa/svc/authen/login.do");
                byte[] bytes = str8.getBytes();
                try {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(15000);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                    httpURLConnection.getOutputStream().write(bytes);
                    if (httpURLConnection.getResponseCode() != 200) {
                        throw new Exception("服务器返回数据错误，认证失败！");
                    }
                    InputStream inputStream = httpURLConnection.getInputStream();
                    StringBuilder sb4 = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));
                    while (true) {
                        String readLine = bufferedReader.readLine();
                        if (readLine == null) {
                            break;
                        }
                        sb4.append(readLine);
                    }
                    Log.d("test", ((Object) sb4) + " result");
                    try {
                        JSONObject jSONObject = new JSONObject(sb4.toString());
                        if (!jSONObject.has(IaaaWrapper.RESULT_SUCCESS)) {
                            throw new Exception("认证失败！");
                        }
                        if (jSONObject.getString(IaaaWrapper.RESULT_SUCCESS).equals("true")) {
                            if (!jSONObject.has("token")) {
                                throw new Exception("认证失败！");
                            }
                            String string = jSONObject.getString("token");
                            if (string.equals("")) {
                                throw new Exception("认证失败！");
                            }
                            return string;
                        }
                        if (!jSONObject.has("errMsg")) {
                            throw new Exception("认证失败！");
                        }
                        throw new Exception("认证失败！" + jSONObject.getString("errMsg"));
                    } catch (JSONException unused) {
                        throw new Exception("服务器数据错误，认证失败！");
                    }
                } catch (IOException unused2) {
                    throw new Exception("服务器返回数据错误，认证失败！");
                }
            } catch (MalformedURLException unused3) {
                throw new Exception("URL错误！");
            }
        } catch (Exception error) {
            Log.e("iAAA", "login request failed: " + error.getMessage(), error);
            throw error;
        }
        */
    }

    static String e(String str, String str2) throws Exception {
        return signedAuthMode(str, str2);
        /*
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("userName", str);
        treeMap.put("appId", str2);
        StringBuilder sb = new StringBuilder();
        for (String str3 : treeMap.keySet()) {
            String str4 = (String) treeMap.get(str3);
            StringBuilder sb2 = sb.length() != 0 ? new StringBuilder("&") : new StringBuilder();
            sb2.append(str3);
            sb2.append("=");
            sb2.append(str4);
            sb.append(sb2.toString());
        }
        String sb3 = sb.toString();
        try {
            String b2 = b(sb3 + "7696baa1fa4ed9679441764a271e556e");
            if (b2 == null) {
                throw new Exception("数据处理错误！");
            }
            String str5 = sb3 + "&msgAbs=" + b2;
            try {
                URL url = new URL("https://iaaa.pku.edu.cn/iaaa/svc/authen/isMobileAuthen.do");
                byte[] bytes = str5.getBytes();
                try {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(15000);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                    httpURLConnection.getOutputStream().write(bytes);
                    if (httpURLConnection.getResponseCode() != 200) {
                        throw new Exception("服务器返回数据错误，查验验证选项失败！");
                    }
                    InputStream inputStream = httpURLConnection.getInputStream();
                    StringBuilder sb4 = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));
                    while (true) {
                        String readLine = bufferedReader.readLine();
                        if (readLine == null) {
                            break;
                        }
                        sb4.append(readLine);
                    }
                    JSONObject jSONObject = new JSONObject(sb4.toString());
                    Log.d("iAAA", "auth-mode response: success=" + jSONObject.optString(IaaaWrapper.RESULT_SUCCESS)
                            + ", authenMode=" + jSONObject.optString("authenMode")
                            + ", isBind=" + jSONObject.optString("isBind")
                            + ", errMsg=" + jSONObject.optString("errMsg"));
                    if (!jSONObject.has(IaaaWrapper.RESULT_SUCCESS)) {
                        if (!jSONObject.has("errMsg")) {
                            throw new Exception("查验验证选项失败！");
                        }
                        throw new Exception("查验验证选项失败！" + jSONObject.getString("errMsg"));
                    }
                    if (!jSONObject.getString(IaaaWrapper.RESULT_SUCCESS).equals("true")) {
                        if (jSONObject.has("errMsg")) {
                            throw new Exception("查验验证选项失败！" + jSONObject.getString("errMsg"));
                        }
                        throw new Exception("查验验证选项失败！");
                    }
                    if (!jSONObject.has("authenMode")) {
                        throw new Exception("查验验证选项失败！");
                    }
                    String string = jSONObject.getString("authenMode");
                    if (string.equals("SMS")) {
                        return "SMS";
                    }
                    if (!string.equals("OTP")) {
                        return "";
                    }
                    if (!jSONObject.has("isBind")) {
                        throw new Exception("如果是动态口令验证但是没有绑定， 通过页面（https://iaaa.pku.edu.cn/iaaa/login2OTP.jsp）登录，按照要求完成绑定。");
                    }
                    if (Boolean.valueOf(jSONObject.getBoolean("isBind")).booleanValue()) {
                        return "OTP";
                    }
                    throw new Exception("如果是动态口令验证但是没有绑定， 通过页面（https://iaaa.pku.edu.cn/iaaa/login2OTP.jsp）登录，按照要求完成绑定。");
                } catch (IOException unused2) {
                    throw new Exception("服务器返回数据错误，查验验证选项失败！");
                }
            } catch (MalformedURLException unused3) {
                throw new Exception("URL错误！");
            }
        } catch (Exception error) {
            Log.e("iAAA", "auth-mode request failed: " + error.getMessage(), error);
            throw error;
        }
        */
    }
}
