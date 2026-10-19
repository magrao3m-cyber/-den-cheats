package com.example.edencheats;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Tabela Title ID -> nome do jogo (embutida no APK + extras baixados do README do repositório). */
final class Names {
    private Names() {}

    static final String README_URL =
            "https://raw.githubusercontent.com/ADEMOLA200/Switch-Emulator-Mod-Database/develop/README.md";
    private static final Pattern TIDRE = Pattern.compile("\\b(01[0-9A-Fa-f]{14})\\b");

    private static final Map<String, String> BY_TID = new HashMap<>();
    private static final Map<String, String> BY_NAME = new HashMap<>();
    private static boolean loaded = false;

    static synchronized void ensure(Context c) {
        if (loaded && !BY_TID.isEmpty()) return;

        // 1. Carrega diretamente de assets/names.tsv (direto do APK, instantâneo e à prova de falhas)
        try (InputStream in = c.getAssets().open("names.tsv")) {
            parseTsv(in);
        } catch (Exception ignored) {
            // Continua para tentar fontes secundárias
        }

        // 2. Tenta carregar do zip embutido caso assets/names.tsv não tenha sido lido
        if (BY_TID.isEmpty()) {
            try {
                File z = Sources.assetZip(c);
                try (ZipFile zf = new ZipFile(z)) {
                    ZipEntry e = zf.getEntry("names.tsv");
                    if (e != null) {
                        try (InputStream in = zf.getInputStream(e)) {
                            parseTsv(in);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Lê nomes extras baixados ou atualizados em tempo de execução
        File x = new File(c.getFilesDir(), "names_extra.tsv");
        if (x.exists()) {
            try (InputStream in = new FileInputStream(x)) {
                parseTsv(in);
            } catch (IOException ignored) {
            }
        }
        loaded = true;
    }

    private static void parseTsv(InputStream in) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) {
                l = l.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                int t = l.indexOf('\t');
                if (t < 0) t = l.indexOf('|');
                if (t >= 16) {
                    String tid = l.substring(0, t).trim();
                    String name = l.substring(t + 1).trim();
                    if (Util.isHex16(tid) && !name.isEmpty()) {
                        put(tid.toUpperCase(Locale.US), name);
                    }
                }
            }
        }
    }

    static synchronized void put(String tid, String name) {
        if (name == null || name.isEmpty()) return;
        BY_TID.put(tid, name);
        BY_NAME.put(norm(name), tid);
    }

    static synchronized String get(String tid) {
        if (tid == null || tid.isEmpty()) return null;
        String t = tid.trim().toUpperCase(Locale.US);
        String name = BY_TID.get(t);
        if (name != null) return name;

        // Se for um Title ID de update (termina com 800), busca pelo ID base (000)
        if (t.length() == 16 && t.startsWith("0100") && t.endsWith("800")) {
            String base = t.substring(0, 13) + "000";
            String baseName = BY_TID.get(base);
            if (baseName != null) return baseName;
        }

        // Se for ID base (000) e não achou, tenta pelo update (800)
        if (t.length() == 16 && t.startsWith("0100") && t.endsWith("000")) {
            String upd = t.substring(0, 13) + "800";
            String updName = BY_TID.get(upd);
            if (updName != null) return updName;
        }

        return null;
    }

    static synchronized String tidFor(String name) {
        return BY_NAME.get(norm(name));
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** Baixa o README do repositório de mods e extrai pares nome/TID da tabela de jogos. */
    static void refreshFromReadme(Context c, boolean force, Util.Progress p) throws IOException {
        File x = new File(c.getFilesDir(), "names_extra.tsv");
        if (!force && x.exists()) return;
        if (p != null) p.onProgress("Baixando nomes dos jogos…");
        String md = new String(Util.httpBytes(README_URL, null), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (String line : md.split("\n")) {
            if (!line.startsWith("|")) continue;
            Matcher m = TIDRE.matcher(line);
            if (!m.find()) continue;
            String tid = m.group(1).toUpperCase(Locale.US);
            String name = null;
            for (String cell : line.split("\\|")) {
                String s = clean(cell);
                if (s.isEmpty() || s.matches("\\d+") || Util.isHex16(s)) continue;
                name = s;
                break;
            }
            if (name == null) continue;
            put(tid, name);
            sb.append(tid).append('\t').append(name.replace('\t', ' ')).append('\n');
        }
        Util.writeFile(x, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String clean(String cell) {
        String s = cell.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
        s = s.replace("*", "").replace("`", "").trim();
        return s;
    }
}
