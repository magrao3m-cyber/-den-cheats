package com.example.edencheats;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Procura no aparelho o emulador Eden (e forks compatíveis) já instalado, para o SwitchForge
 * sugerir a configuração da pasta sozinho. Usa apenas o PackageManager (sem permissões especiais):
 * o Android não deixa um app vasculhar a pasta de dados de outro app, então a detecção descobre
 * QUAL app é o Eden e o seletor de pastas é aberto já apontando para ele.
 */
final class EdenDetector {
    private EdenDetector() {}

    static final class Found {
        final String pkg;
        final String label;
        final int score;

        Found(String pkg, String label, int score) {
            this.pkg = pkg;
            this.label = label;
            this.score = score;
        }
    }

    private static final String PREFS = "eden_setup";
    private static final String KEY_ASKED = "setup_asked";
    private static final String KEY_LAST_SCAN = "last_scan";

    /** "eden" como palavra inteira (evita falsos positivos como "garden"). */
    private static final Pattern EDEN_WORD = Pattern.compile("(?i)(^|[^a-z])eden([^a-z]|$)");

    /** Forks conhecidos que usam a mesma estrutura de pastas (files/load/...). */
    private static final String[] KNOWN_FORKS = {
            "org.yuzu.yuzu_emu",
            "org.yuzu.yuzu_emu.ea",
            "org.sudachi.sudachi_emu",
            "org.citron.citron_emu"
    };

    /** Apps de emulador encontrados, do mais provável (Eden oficial) para o menos provável. */
    static List<Found> detect(Context c) {
        List<Found> out = new ArrayList<>();
        try {
            PackageManager pm = c.getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg == null || pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
                String label = String.valueOf(ri.loadLabel(pm));
                int score = score(pkg, label);
                if (score >= 0) out.add(new Found(pkg, label, score));
            }
        } catch (Exception ignored) {
            // sem acesso à lista de apps: segue sem detecção automática
        }
        Collections.sort(out, (a, b) -> a.score != b.score
                ? Integer.compare(a.score, b.score)
                : a.label.compareToIgnoreCase(b.label));
        return out;
    }

    private static int score(String pkg, String label) {
        if (SafStore.DEFAULT_PKG.equals(pkg)) return 0;
        if (EDEN_WORD.matcher(pkg.replace('.', ' ').replace('_', ' ')).find() || EDEN_WORD.matcher(label).find()) return 1;
        for (String k : KNOWN_FORKS) if (k.equals(pkg)) return 2;
        return -1;
    }

    /** Melhor candidato ou null. */
    static Found best(Context c) {
        List<Found> f = detect(c);
        return f.isEmpty() ? null : f.get(0);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Já mostramos o assistente de configuração automática alguma vez? */
    static boolean wasAsked(Context c) {
        return prefs(c).getBoolean(KEY_ASKED, false);
    }

    /** Quando foi a última varredura de jogos (ms), ou 0 se nunca. */
    static long lastScan(Context c) {
        return prefs(c).getLong(KEY_LAST_SCAN, 0L);
    }

    static void markScanned(Context c) {
        prefs(c).edit().putLong(KEY_LAST_SCAN, System.currentTimeMillis()).apply();
    }

    static void markAsked(Context c) {
        prefs(c).edit().putBoolean(KEY_ASKED, true).apply();
    }
}
