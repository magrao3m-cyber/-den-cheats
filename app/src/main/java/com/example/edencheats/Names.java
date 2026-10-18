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
        if (loaded) return;
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
            // sem nomes embutidos: segue só com Title IDs
        }
        File x = new File(c.getFilesDir(), "names_extra.tsv");
        if (x.exists()) {
            try (InputStream in = new FileInputStream(x)) {
                parseTsv(in);
            } catch (IOException ignored) {
                // ignora cache corrompido
            }
        }
        loaded = true;
    }

    private static void parseTsv(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String l;
        while ((l = r.readLine()) != null) {
            int t = l.indexOf('\t');
            if (t == 16) {
                put(l.substring(0, 16).toUpperCase(Locale.US), l.substring(t + 1).trim());
            }
        }
    }

    static synchronized void put(String tid, String name) {
        if (name == null || name.isEmpty()) return;
        BY_TID.put(tid, name);
        BY_NAME.put(norm(name), tid);
    }

    static synchronized String get(String tid) {
        return BY_TID.get(tid);
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
