package com.example.edencheats;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Servidores de cheats e mods:
 * - Server 1 (Offline · Base Principal Embutida)
 * - Server 2 (Nuvem · Alta Velocidade)
 * - Server 3 (Nuvem · Mirror Global)
 * - Server 4 (Nuvem · Repositório Alternativo)
 * - Server 5 (Nuvem · 60FPS & Resolução)
 * - Server 6 (Nuvem · Mods & Otimizações)
 * - Server 7 (Nuvem · Curadoria Especial)
 */
final class Sources {
    private Sources() {}

    static final String ASSET_ZIP = "ibnux_db.zip";
    static final String ASSET_CACHE = "offline_db_v5.zip";

    private static String decode(String b64) {
        return new String(android.util.Base64.decode(b64, android.util.Base64.NO_WRAP), StandardCharsets.UTF_8);
    }

    private static final String S2_URL = decode("aHR0cHM6Ly9naXRodWIuY29tL0hhbWxldER1RnJvbWFnZS9zd2l0Y2gtY2hlYXRzLWRiL3JlbGVhc2VzL2xhdGVzdC9kb3dubG9hZC8=");
    private static final String S3_URL = decode("aHR0cHM6Ly9naXRodWIuY29tL2V4ZWZlci9nYmF0ZW1wLW1hdGlhczNkcy1jaGVhdHMvYXJjaGl2ZS9yZWZzL2hlYWRzL21hc3Rlci56aXA=");
    private static final String S4_URL = decode("aHR0cHM6Ly9naXRodWIuY29tL3RvbXZpdGEvTlhDaGVhdENvZGUvcmVsZWFzZXMvbGF0ZXN0L2Rvd25sb2FkL3RpdGxlcy56aXA=");
    private static final String S6_REPO = decode("QURFTU9MQTIwMC9Td2l0Y2gtRW11bGF0b3ItTW9kLURhdGFiYXNl");
    private static final String S7_REPO = decode("YmFkMWRlYS9OWENoZWF0cw==");

    abstract static class Source {
        final String label;
        final String defaultMod;

        Source(String label, String defaultMod) {
            this.label = label;
            this.defaultMod = defaultMod;
        }

        abstract List<Model.Game> load(Context c, boolean refresh, Util.Progress p) throws Exception;
    }

    static Source[] all() {
        return new Source[]{
                new ZipSource("Server 1 (Offline · Base Principal)", "Cheats", null, ASSET_CACHE, true),
                new ZipSource("Server 2 (Nuvem · Alta Velocidade)", "Cheats",
                        S2_URL + "contents_complete.zip", "server2_db.zip", false),
                new ZipSource("Server 3 (Nuvem · Mirror Global)", "Cheats",
                        S3_URL, "server3_db.zip", false),
                new ZipSource("Server 4 (Nuvem · Repositório Alternativo)", "Cheats",
                        S4_URL, "server4_db.zip", false),
                new ZipSource("Server 5 (Nuvem · 60FPS & Resolução)", "Cheats 60FPS-GFX",
                        S2_URL + "contents_60fps-res-gfx.zip", "server5_gfx.zip", false),
                new RepoSource("Server 6 (Nuvem · Mods & Otimizações)", S6_REPO, "develop"),
                new RepoSource("Server 7 (Nuvem · Curadoria Especial)", S7_REPO, "main"),
        };
    }

    /** Copia o ZIP embutido para a pasta interna (uma vez) e o devolve. */
    static synchronized File assetZip(Context c) throws IOException {
        File f = new File(c.getFilesDir(), ASSET_CACHE);
        if (f.exists() && f.length() > 0) return f;
        try (InputStream in = c.getAssets().open(ASSET_ZIP); OutputStream out = new FileOutputStream(f)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        return f;
    }

    /** Lê o conteúdo de um arquivo de um item (do ZIP local ou por HTTP). */
    static byte[] read(Model.Item it, Model.FileRef r) throws IOException {
        if (r.url != null) return Util.httpBytes(r.url, null);
        try (ZipFile zf = new ZipFile(it.zip)) {
            ZipEntry e = zf.getEntry(r.zipPath);
            if (e == null) throw new IOException("Entrada não encontrada: " + r.zipPath);
            try (InputStream in = zf.getInputStream(e)) {
                return Util.readAll(in);
            }
        }
    }

    static void sortGames(List<Model.Game> list) {
        Collections.sort(list, new Comparator<Model.Game>() {
            @Override
            public int compare(Model.Game a, Model.Game b) {
                return a.display().compareToIgnoreCase(b.display());
            }
        });
    }

    // ---------------------------------------------------------------- ZIP

    static final class ZipSource extends Source {
        static final Pattern ENTRY = Pattern.compile(
                "(?:^|/)\\[?([0-9A-Fa-f]{16})\\]?/(?:cheats/)?([0-9A-Fa-f]{16})\\.txt$");

        final String url;
        final String cacheName;
        final boolean fromAsset;

        ZipSource(String label, String defaultMod, String url, String cacheName, boolean fromAsset) {
            super(label, defaultMod);
            this.url = url;
            this.cacheName = cacheName;
            this.fromAsset = fromAsset;
        }

        @Override
        List<Model.Game> load(Context c, boolean refresh, Util.Progress p) throws Exception {
            Names.ensure(c);
            File f;
            if (fromAsset) {
                f = assetZip(c);
            } else {
                f = new File(c.getFilesDir(), cacheName);
                if (refresh || !f.exists() || f.length() == 0) {
                    p.onProgress("Sincronizando com o servidor na nuvem…");
                    Util.download(url, f, p);
                }
            }
            p.onProgress("Processando catálogo de cheats…");
            return index(f);
        }

        List<Model.Game> index(File f) throws IOException {
            Map<String, Model.Game> map = new TreeMap<>();
            try (ZipFile zf = new ZipFile(f)) {
                Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory()) continue;
                    Matcher m = ENTRY.matcher(e.getName());
                    if (!m.find()) continue;
                    String tid = m.group(1).toUpperCase(Locale.US);
                    String bid = m.group(2).toUpperCase(Locale.US);
                    Model.Game g = map.get(tid);
                    if (g == null) {
                        g = new Model.Game(tid, Names.get(tid));
                        map.put(tid, g);
                    }
                    Model.Item it = new Model.Item();
                    it.cheat = true;
                    it.bid = bid;
                    it.modName = defaultMod;
                    it.zip = f;
                    it.title = "Cheats · Build ID " + bid;
                    it.files.add(new Model.FileRef("cheats/" + bid + ".txt", null, e.getName(), e.getSize()));
                    g.items.add(it);
                }
            }
            List<Model.Game> out = new ArrayList<>(map.values());
            for (Model.Game g : out) {
                Collections.sort(g.items, new Comparator<Model.Item>() {
                    @Override
                    public int compare(Model.Item a, Model.Item b) {
                        return a.bid.compareTo(b.bid);
                    }
                });
            }
            sortGames(out);
            return out;
        }
    }

    // ---------------------------------------------------------------- Repositório GitHub

    static final class RepoSource extends Source {
        static final Pattern TIDSEG = Pattern.compile("^\\[?(01[0-9A-Fa-f]{14})\\]?$");
        static final Pattern TID_PREFIX = Pattern.compile("^(01[0-9A-Fa-f]{14})(?:\\s+(.+))?$");

        final String repo;
        final String branch;
        final String cacheKey;

        RepoSource(String label, String repo, String branch) {
            super(label, "Mod");
            this.repo = repo;
            this.branch = branch;
            this.cacheKey = "repo_tree_" + repo.replace('/', '_') + ".json";
        }

        @Override
        List<Model.Game> load(Context c, boolean refresh, Util.Progress p) throws Exception {
            Names.ensure(c);
            File tree = new File(c.getFilesDir(), cacheKey);
            if (refresh || !tree.exists() || tree.length() == 0) {
                p.onProgress("Conectando ao servidor na nuvem…");
                byte[] b = Util.httpBytes("https://api.github.com/repos/" + repo
                        + "/git/trees/" + branch + "?recursive=1", "application/vnd.github+json");
                Util.writeFile(tree, b);
            }
            if (S6_REPO.equalsIgnoreCase(repo)) {
                try {
                    Names.refreshFromReadme(c, refresh, p);
                } catch (IOException ignored) {
                    // nomes são opcionais
                }
            }
            p.onProgress("Sincronizando itens do servidor…");
            JSONObject root = new JSONObject(new String(Util.readFile(tree), StandardCharsets.UTF_8));
            return parse(root.getJSONArray("tree"));
        }

        List<Model.Game> parse(JSONArray arr) throws Exception {
            final String raw = "https://raw.githubusercontent.com/" + repo + "/" + branch + "/";
            Map<String, Model.Game> games = new TreeMap<>();
            Map<String, Model.Item> items = new HashMap<>();

            for (int k = 0; k < arr.length(); k++) {
                JSONObject o = arr.getJSONObject(k);
                if (!"blob".equals(o.optString("type"))) continue;
                String path = o.getString("path");
                long size = o.optLong("size", 0);
                String[] seg = path.split("/");
                if (seg.length < 2) continue;

                String tid = null;
                String nameCandidate = null;
                for (String s : seg) {
                    Matcher m = TID_PREFIX.matcher(s);
                    if (m.matches()) {
                        tid = m.group(1).toUpperCase(Locale.US);
                        if (m.groupCount() >= 2 && m.group(2) != null && !m.group(2).trim().isEmpty()) {
                            nameCandidate = m.group(2).trim();
                        }
                        break;
                    }
                }
                String folder = seg[0];
                String gameName = nameCandidate != null ? nameCandidate : folder;
                if (tid == null) {
                    tid = Names.tidFor(folder);
                } else if (folder.equalsIgnoreCase("titles") || folder.equalsIgnoreCase("contents")) {
                    gameName = Names.get(tid);
                } else if (nameCandidate == null && tid != null) {
                    String n = Names.get(tid);
                    if (n != null && !n.startsWith("?")) gameName = n;
                }

                int j = -1;
                for (int i = 0; i < seg.length - 1; i++) {
                    String s = seg[i].toLowerCase(Locale.US);
                    if (s.equals("cheats") || s.equals("romfs") || s.equals("exefs") || s.equals("exefs_patches")) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) continue;
                String marker = seg[j].toLowerCase(Locale.US);

                String ver = null;
                if (j - 1 >= 1) {
                    String prev = seg[j - 1];
                    if (!TIDSEG.matcher(prev).matches() && !prev.equalsIgnoreCase("titles")
                            && !prev.equalsIgnoreCase("contents") && !prev.equalsIgnoreCase("atmosphere")) {
                        ver = prev;
                    }
                }
                String suffix = ver != null ? " " + ver : "";
                String[] rest = Arrays.copyOfRange(seg, j + 1, seg.length);

                boolean cheat = false;
                String bid = "";
                String modName;
                String dest;
                if (marker.equals("cheats")) {
                    String last = rest[rest.length - 1];
                    String base = last.toLowerCase(Locale.US).endsWith(".txt")
                            ? last.substring(0, last.length() - 4) : "";
                    if (rest.length == 1 && Util.isHex16(base)) {
                        cheat = true;
                        bid = base.toUpperCase(Locale.US);
                        modName = "Cheats";
                        dest = "cheats/" + bid + ".txt";
                    } else if (rest.length == 1 && Util.isHex16(last)) {
                        cheat = true;
                        bid = last.toUpperCase(Locale.US);
                        modName = "Cheats";
                        dest = "cheats/" + bid + ".txt";
                    } else {
                        // Arquivos secundários como toggles.txt são ignorados
                        continue;
                    }
                } else if (marker.equals("romfs")) {
                    modName = "romfs" + suffix;
                    dest = "romfs/" + Util.join(rest, 0);
                } else if (marker.equals("exefs")) {
                    modName = "exefs" + suffix;
                    dest = "exefs/" + Util.join(rest, 0);
                } else {
                    if (rest.length < 2) continue;
                    modName = rest[0] + suffix;
                    dest = "exefs/" + Util.join(rest, 1);
                }

                String gameKey = tid != null ? tid : "?" + folder;
                Model.Game g = games.get(gameKey);
                if (g == null) {
                    g = new Model.Game(tid != null ? tid : "", gameName);
                    games.put(gameKey, g);
                } else if ((g.name == null || g.name.startsWith("?")) && gameName != null && !gameName.startsWith("?")) {
                    g.name = gameName;
                }

                String itemKey = gameKey + "|" + (cheat ? "cheat|" + bid : "mod|" + modName);
                Model.Item it = items.get(itemKey);
                if (it == null) {
                    it = new Model.Item();
                    it.cheat = cheat;
                    it.bid = bid;
                    it.modName = modName;
                    it.title = cheat ? "Cheats · Build ID " + bid : "Mod · " + modName;
                    items.put(itemKey, it);
                    g.items.add(it);
                }
                if (cheat && !it.files.isEmpty()) continue;
                it.files.add(new Model.FileRef(dest, raw + Util.encodePath(path), null, size));
            }

            List<Model.Game> out = new ArrayList<>(games.values());
            sortGames(out);
            return out;
        }
    }
}
