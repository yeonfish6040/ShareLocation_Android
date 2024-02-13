package com.yeonfish.sharelocation.util;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    private HttpUtil(){}
    public static HttpUtil getInstance(){
        return LazyHolder.INSTANCE;
    }
    private static class LazyHolder{
        private static final HttpUtil INSTANCE=new HttpUtil();
    }

    public JSONObject get(String urlStr) {
        try {
            //get 요청할 url을 적어주시면 됩니다. 형태를 위해 저는 그냥 아무거나 적은 겁니다.
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000); //서버에 연결되는 Timeout 시간 설정
            con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
            con.setRequestMethod("GET");

            //URLConnection에 대한 doOutput 필드값을 지정된 값으로 설정합니다.
            //URL 연결은 입출력에 사용될 수 있어요.
            //URL 연결을 출력용으로 사용하려는 경우 DoOutput 플래그를 true로 설정하고,
            //그렇지 않은 경우는 false로 설정하세요. 기본값은 false입니다.
            con.setDoOutput(false);

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                JSONObject responseData=new JSONObject(sb.toString());
                System.out.println("" + sb.toString());
                return responseData;
            } else {
                System.out.println(con.getResponseMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
        return null;
    }

    public String post(String urlStr, String data, @Nullable String[][] property){
        try {
            //post 요청할 url을 적어주시면 됩니다. 형태를 위해 저는 그냥 아무거나 적은 겁니다.
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000); //서버에 연결되는 Timeout 시간 설정
            con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
            con.setRequestMethod("POST"); //어떤 요청으로 보낼 것인지?

            //json으로 message를 전달하고자 할 때
//            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", Integer.toString(data.length()));
            con.setRequestProperty("charset", "utf-8");
            if (property != null) {
                for (String[] i:property) {
                    con.setRequestProperty(i[0], i[1]);
                }
            }

            con.setDoInput(true);
            con.setDoOutput(true); //POST 데이터를 OutputStream으로 넘겨 주겠다는 설정
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);

            try (DataOutputStream dos = new DataOutputStream(con.getOutputStream())) {
                dos.writeBytes(data);
            }

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return sb.toString();
            } else {
                System.out.println(con.getResponseMessage());
                Log.d("Res", url.toString());
            }
        } catch (Exception e){
//            System.err.println(e.toString());
            e.printStackTrace();
        }
        return null;
    }
}

