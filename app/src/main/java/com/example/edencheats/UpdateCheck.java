package com.example.edencheats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Avisa quando existe uma versão mais nova do app publicada nos releases do GitHub. */
final class UpdateCheck {
    private UpdateCheck() {}

    private static final String API = "https://api.github.com/repos/magrao3m-cyber/-den-cheats/releases/latest";
    private static final long EVERY_MS = 12L * 60 * 60 * 1000;

    static final class Info {
        final String tag;
        final String url;

        Info(String tag, String url) {
            this.tag = tag;
            this.url = url;
        }
    }

    interface Callback {
        void onNewVersion(Info info);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("update_check", Context.MODE_PRIVATE);
    }

    /** true se "v1.8.1" é mais novo que "1.8.0". */
    static boolean isNewer(String tag, String current) {
        int[] a = parts(tag);
        int[] b = parts(current);
        for (int i = 0; i < 4; i++) {
            if (a[i] != b[i]) return a[i] > b[i];
        }
        return false;
    }

    private static int[] parts(String v) {
        int[] out = new int[4];
        if (v == null) return out;
        String[] seg = v.replaceAll("[^0-9.]", "").split("\\.");
        for (int i = 0; i < seg.length && i < 4; i++) {
            try {
                out[i] = Integer.parseInt(seg[i]);
            } catch (Exception ignored) {
                out[i] = 0;
            }
        }
        return out;
    }

    /** Versão nova já conhecida (do último teste) e ainda não dispensada, ou null. */
    static Info cached(Context c, String current) {
        SharedPreferences sp = prefs(c);
        String tag = sp.getString("tag", null);
        if (tag == null || !isNewer(tag, current) || tag.equals(sp.getString("dismissed", ""))) return null;
        return new Info(tag, sp.getString("url", ""));
    }

    static void dismiss(Context c, Info i) {
        prefs(c).edit().putString("dismissed", i.tag).apply();
    }

    /** Consulta o GitHub (no máximo a cada 12 h) e chama o callback na thread principal se houver versão nova. */
    static void check(final Context c, final String current, final Callback cb) {
        final SharedPreferences sp = prefs(c);
        if (System.currentTimeMillis() - sp.getLong("last", 0) < EVERY_MS) return;
        Util.POOL.execute(() -> {
            try {
                byte[] b = Util.httpBytes(API, "application/vnd.github+json");
                JSONObject o = new JSONObject(new String(b, StandardCharsets.UTF_8));
                String tag = o.optString("tag_name", "");
                String url = o.optString("html_url", "");
                sp.edit().putLong("last", System.currentTimeMillis()).putString("tag", tag).putString("url", url).apply();
                final Info info = cached(c, current);
                if (info != null) new Handler(Looper.getMainLooper()).post(() -> cb.onNewVersion(info));
            } catch (Exception ignored) {
                // sem internet: tenta de novo na próxima abertura
            }
        });
    }
}
