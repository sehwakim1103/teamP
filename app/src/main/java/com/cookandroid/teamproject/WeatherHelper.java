package com.cookandroid.teamproject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherHelper {


    private static final String API_KEY = "3a96a752fe207af7bbef079650ffb0acd428a2bcb59e478dc1445a223b5b07b1";

    public interface Callback {
        void onResult(String temp, int pty);
        void onError(String msg);
    }

    public static void fetch(Callback cb) {
        new Thread(() -> {
            try {
                TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                Calendar c = Calendar.getInstance(tz);

                if (c.get(Calendar.MINUTE) < 10) c.add(Calendar.HOUR_OF_DAY, -1);

                SimpleDateFormat dF = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
                SimpleDateFormat tF = new SimpleDateFormat("HH00", Locale.KOREA);
                dF.setTimeZone(tz);
                tF.setTimeZone(tz);
                String baseDate = dF.format(c.getTime());
                String baseTime = tF.format(c.getTime());

                String urlStr = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst"
                        + "?serviceKey=" + URLEncoder.encode(API_KEY, "UTF-8")
                        + "&pageNo=1&numOfRows=10&dataType=JSON"
                        + "&base_date=" + baseDate
                        + "&base_time=" + baseTime
                        + "&nx=60&ny=127";

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String body = sb.toString();


                if (!body.trim().startsWith("{")) {
                    cb.onError(body.length() > 120 ? body.substring(0, 120) : body);
                    return;
                }

                JSONArray items = new JSONObject(body)
                        .getJSONObject("response").getJSONObject("body")
                        .getJSONObject("items").getJSONArray("item");

                String temp = "-";
                int pty = 0;
                for (int i = 0; i < items.length(); i++) {
                    JSONObject o = items.getJSONObject(i);
                    String cat = o.getString("category");
                    if (cat.equals("T1H")) temp = o.getString("obsrValue");
                    if (cat.equals("PTY")) pty = (int) Double.parseDouble(o.getString("obsrValue"));
                }
                cb.onResult(temp, pty);
            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? "알 수 없는 오류" : e.getMessage());
            }
        }).start();
    }
}