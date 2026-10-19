package com.example.edencheats;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cliente de integração com a API do CheatSlips.com.
 * Permite buscar títulos de cheats e código completo caso o usuário configure seu API Token.
 */
public final class CheatSlips {
    private CheatSlips() {}

    private static final String PREF = "cheatslips_prefs";
    private static final String KEY_TOKEN = "api_token";
    public static final String SITE_URL = "https://www.cheatslips.com";
    private static final String API_BASE = "https://www.cheatslips.com/api/v1/cheats/";

    public static String getToken(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
    }

    public static void setToken(Context c, String token) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token != null ? token.trim() : "")
                .apply();
    }

    public static boolean hasToken(Context c) {
        String t = getToken(c);
        return t != null && !t.trim().isEmpty();
    }

    public static class CheatResult {
        public final String titleId;
        public final String buildId;
        public final List<String> cheatTitles = new ArrayList<>();
        public String fullContent = null;
        public boolean requiresToken = false;
        public boolean quotaExceeded = false;
        public String errorMessage = null;

        public CheatResult(String tid, String bid) {
            this.titleId = tid;
            this.buildId = bid;
        }
    }

    public static CheatResult fetch(Context c, String tid, String bid) {
        String cleanTid = tid.toUpperCase(Locale.US);
        String cleanBid = bid.toUpperCase(Locale.US);
        CheatResult res = new CheatResult(cleanTid, cleanBid);

        String token = getToken(c);
        String urlStr = API_BASE + cleanTid + "/" + cleanBid.toLowerCase(Locale.US);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("User-Agent", "EdenCheatsManager/1.4.0");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("X-API-TOKEN", token);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code != 200) {
                res.errorMessage = "Servidor CheatSlips retornou status HTTP " + code;
                return res;
            }

            String jsonStr;
            try (InputStream in = conn.getInputStream()) {
                jsonStr = new String(Util.readAll(in), StandardCharsets.UTF_8);
            }

            JSONObject obj = new JSONObject(jsonStr);
            JSONArray cheats = obj.optJSONArray("cheats");
            if (cheats == null || cheats.length() == 0) {
                res.errorMessage = "Nenhum cheat encontrado no CheatSlips para a Build ID " + cleanBid;
                return res;
            }

            StringBuilder sbContent = new StringBuilder();
            for (int i = 0; i < cheats.length(); i++) {
                JSONObject ch = cheats.getJSONObject(i);
                JSONArray titles = ch.optJSONArray("titles");
                if (titles != null) {
                    for (int j = 0; j < titles.length(); j++) {
                        String t = titles.getString(j);
                        if (!res.cheatTitles.contains(t)) res.cheatTitles.add(t);
                    }
                }
                String content = ch.optString("content", "");
                if (content.contains("Please register and send APIToken")) {
                    res.requiresToken = true;
                } else if (content.contains("Quota exceeded")) {
                    res.quotaExceeded = true;
                } else if (!content.trim().isEmpty()) {
                    sbContent.append(content).append("\n\n");
                }
            }

            if (sbContent.length() > 0) {
                res.fullContent = sbContent.toString().trim();
            }

        } catch (Exception e) {
            res.errorMessage = "Falha ao conectar com CheatSlips: " + e.getMessage();
        }

        return res;
    }
}
