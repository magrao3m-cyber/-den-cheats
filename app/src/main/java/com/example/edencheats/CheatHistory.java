package com.example.edencheats;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Memória do app por jogo (tudo no aparelho): cheats mais usados, perfis salvos
 * e o "desfazer" da última instalação.
 */
final class CheatHistory {
    private CheatHistory() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("eden_history", Context.MODE_PRIVATE);
    }

    private static JSONObject obj(Context c, String key) {
        try {
            String s = prefs(c).getString(key, null);
            return s == null ? new JSONObject() : new JSONObject(s);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    // ---- Mais usados ----

    /** Nome do cheat -> quantas vezes foi instalado neste jogo. */
    static Map<String, Integer> usage(Context c, String tid) {
        Map<String, Integer> out = new LinkedHashMap<>();
        JSONObject o = obj(c, "usage_" + tid);
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            out.put(k, o.optInt(k, 0));
        }
        return out;
    }

    static void recordUsage(Context c, String tid, Collection<String> names) {
        try {
            JSONObject o = obj(c, "usage_" + tid);
            for (String n : names) o.put(n, o.optInt(n, 0) + 1);
            prefs(c).edit().putString("usage_" + tid, o.toString()).apply();
        } catch (Exception ignored) {
            // estatística é opcional
        }
    }

    // ---- Perfis ----

    /** Nome do perfil -> nomes dos cheats. */
    static Map<String, List<String>> profiles(Context c, String tid) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        JSONObject o = obj(c, "prof_" + tid);
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            List<String> l = new ArrayList<>();
            JSONArray a = o.optJSONArray(k);
            if (a != null) for (int i = 0; i < a.length(); i++) l.add(a.optString(i));
            out.put(k, l);
        }
        return out;
    }

    static void saveProfile(Context c, String tid, String name, Collection<String> cheats) {
        try {
            JSONObject o = obj(c, "prof_" + tid);
            JSONArray a = new JSONArray();
            for (String s : cheats) a.put(s);
            o.put(name, a);
            prefs(c).edit().putString("prof_" + tid, o.toString()).apply();
        } catch (Exception ignored) {
            // sem perfil, o resto continua funcionando
        }
    }

    static void deleteProfile(Context c, String tid, String name) {
        JSONObject o = obj(c, "prof_" + tid);
        o.remove(name);
        prefs(c).edit().putString("prof_" + tid, o.toString()).apply();
    }

    // ---- Desfazer a última instalação ----

    /** Guarda o que existia antes (null = arquivo não existia) para poder voltar atrás. */
    static void saveLast(Context c, String tid, Map<String, byte[]> previous) {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, byte[]> e : previous.entrySet()) {
                o.put(e.getKey(), e.getValue() == null ? JSONObject.NULL
                        : Base64.encodeToString(e.getValue(), Base64.NO_WRAP));
            }
            prefs(c).edit().putString("last_" + tid, o.toString()).apply();
        } catch (Exception ignored) {
            // sem desfazer, o resto continua funcionando
        }
    }

    static boolean hasLast(Context c, String tid) {
        return prefs(c).getString("last_" + tid, null) != null;
    }

    /** Estado anterior da última instalação: caminho -> bytes antigos (ou null se não existia). */
    static Map<String, byte[]> last(Context c, String tid) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        JSONObject o = obj(c, "last_" + tid);
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            out.put(k, o.isNull(k) ? null : Base64.decode(o.optString(k), Base64.NO_WRAP));
        }
        return out;
    }

    static void clearLast(Context c, String tid) {
        prefs(c).edit().remove("last_" + tid).apply();
    }
}
