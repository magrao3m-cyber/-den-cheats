package com.example.edencheats;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Resumo da base de cheats local (quantos cheats, jogos e versões). O resultado fica em cache. */
final class DbStats {
    private DbStats() {}

    static final class Stats {
        int cheats;
        int games;
        int builds;
    }

    private static final Pattern ENTRY = Pattern.compile(
            "(?:^|/)\\[?([0-9A-Fa-f]{16})\\]?/(?:cheats/)?([0-9A-Fa-f]{16})\\.txt$");

    /** Pode demorar um pouco: chame fora da thread de interface. */
    static synchronized Stats compute(Context c) throws Exception {
        File f = Sources.assetZip(c);
        SharedPreferences sp = c.getSharedPreferences("db_stats", Context.MODE_PRIVATE);
        String key = f.length() + ":" + f.lastModified();
        Stats st = new Stats();
        if (key.equals(sp.getString("key", ""))) {
            st.cheats = sp.getInt("cheats", 0);
            st.games = sp.getInt("games", 0);
            st.builds = sp.getInt("builds", 0);
            if (st.cheats > 0) return st;
        }
        Set<String> tids = new HashSet<>();
        try (ZipFile z = new ZipFile(f)) {
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                Matcher m = ENTRY.matcher(e.getName());
                if (!m.find()) continue;
                String text;
                try (InputStream in = z.getInputStream(e)) {
                    text = new String(Util.readAll(in), StandardCharsets.UTF_8);
                }
                int n = 0;
                for (String line : text.split("\n")) {
                    String t = line.trim();
                    if (t.startsWith("[") && t.endsWith("]")) n++;
                }
                if (n > 0) {
                    st.cheats += n;
                    st.builds++;
                    tids.add(m.group(1).toUpperCase(java.util.Locale.US));
                }
            }
        }
        st.games = tids.size();
        sp.edit().putString("key", key).putInt("cheats", st.cheats)
                .putInt("games", st.games).putInt("builds", st.builds).apply();
        return st;
    }
}
