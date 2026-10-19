package com.example.edencheats;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de varredura inteligente do emulador Eden.
 * Detecta jogos instalados, versões e Build IDs a partir dos logs de execução,
 * pastas de mods/cheats (load/), configurações individuais (config/custom/) e saves (nand/).
 */
public final class EdenScanner {
    private EdenScanner() {}

    private static final String PREFS_NAME = "eden_scanner_cache";
    private static final String KEY_GAMES = "scanned_games_json";

    public static final class ScannedGame {
        public final String tid;         // 16 caracteres hex (ex.: 0100A6301214E000)
        public String bid;               // 16 caracteres hex (ex.: E7E1382D5EB931B0) ou null
        public String title;             // Nome do jogo detectado
        public String version;           // Versão detectada (ex.: 13.0.1)
        public String source;            // "log", "load", "config", "save" ou "rom"
        public long lastSeen;

        public ScannedGame(String tid, String bid, String title, String version, String source) {
            this.tid = tid == null ? "" : tid.toUpperCase(Locale.US);
            this.bid = (bid != null && Util.isHex16(bid)) ? bid.toUpperCase(Locale.US) : null;
            this.title = title;
            this.version = version;
            this.source = source == null ? "unknown" : source;
            this.lastSeen = System.currentTimeMillis();
        }

        JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("tid", tid);
                if (bid != null) obj.put("bid", bid);
                if (title != null) obj.put("title", title);
                if (version != null) obj.put("version", version);
                obj.put("source", source);
                obj.put("lastSeen", lastSeen);
            } catch (Exception ignored) {}
            return obj;
        }

        static ScannedGame fromJson(JSONObject obj) {
            String t = obj.optString("tid", "");
            String b = obj.optString("bid", null);
            String title = obj.optString("title", null);
            String v = obj.optString("version", null);
            String src = obj.optString("source", "unknown");
            ScannedGame g = new ScannedGame(t, b, title, v, src);
            g.lastSeen = obj.optLong("lastSeen", System.currentTimeMillis());
            return g;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Carrega todos os jogos detectados no cache local. */
    public static synchronized Map<String, ScannedGame> getScannedGames(Context c) {
        Map<String, ScannedGame> map = new LinkedHashMap<>();
        String json = prefs(c).getString(KEY_GAMES, null);
        if (json == null || json.trim().isEmpty()) return map;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                ScannedGame g = ScannedGame.fromJson(arr.getJSONObject(i));
                if (!g.tid.isEmpty()) map.put(g.tid, g);
            }
        } catch (Exception ignored) {}
        return map;
    }

    /** Salva o mapa de jogos detectados no cache persistente. */
    public static synchronized void saveScannedGames(Context c, Map<String, ScannedGame> map) {
        JSONArray arr = new JSONArray();
        for (ScannedGame g : map.values()) {
            arr.put(g.toJson());
        }
        prefs(c).edit().putString(KEY_GAMES, arr.toString()).apply();
    }

    /** Normaliza um Title ID de update (terminado em 800) para o Title ID base (terminado em 000). */
    public static String baseTid(String tid) {
        if (tid == null) return "";
        String t = tid.trim().toUpperCase(Locale.US);
        if (t.length() == 16 && t.startsWith("0100") && t.endsWith("800")) {
            return t.substring(0, 13) + "000";
        }
        return t;
    }

    /** Permite fixar o Build ID e versão escolhidos pelo usuário para um jogo. */
    public static synchronized void setUserSelectedBid(Context c, String tid, String bid, String version) {
        if (tid == null || !Util.isHex16(tid)) return;
        Map<String, ScannedGame> games = getScannedGames(c);
        String t = tid.toUpperCase(Locale.US);
        record(games, t, bid, null, version, "user");
        String b = baseTid(t);
        if (!b.equals(t)) record(games, b, bid, null, version, "user");
        saveScannedGames(c, games);
    }

    /** Retorna o Build ID detectado para um Title ID específico, ou null. */
    public static String getBuildId(Context c, String tid) {
        if (tid == null || tid.isEmpty()) return null;
        Map<String, ScannedGame> games = getScannedGames(c);
        String t = tid.toUpperCase(Locale.US);
        ScannedGame g = games.get(t);
        if (g != null && g.bid != null && Util.isHex16(g.bid)) return g.bid;
        String b = baseTid(t);
        ScannedGame gb = games.get(b);
        if (gb != null && gb.bid != null && Util.isHex16(gb.bid)) return gb.bid;
        return null;
    }

    /** Retorna a versão detectada para um Title ID específico, ou null. */
    public static String getVersion(Context c, String tid) {
        if (tid == null || tid.isEmpty()) return null;
        Map<String, ScannedGame> games = getScannedGames(c);
        String t = tid.toUpperCase(Locale.US);
        ScannedGame g = games.get(t);
        if (g != null && g.version != null && !g.version.isEmpty()) return g.version;
        String b = baseTid(t);
        ScannedGame gb = games.get(b);
        if (gb != null && gb.version != null && !gb.version.isEmpty()) return gb.version;
        return null;
    }

    /** Verifica se o jogo está instalado / foi detectado no Eden. */
    public static boolean isInstalled(Context c, String tid) {
        if (tid == null || tid.isEmpty()) return false;
        Map<String, ScannedGame> games = getScannedGames(c);
        String t = tid.toUpperCase(Locale.US);
        return games.containsKey(t) || games.containsKey(baseTid(t));
    }

    /** Quantidade de jogos detectados no Eden. */
    public static int getCount(Context c) {
        return getScannedGames(c).size();
    }

    /** Limpa o histórico de jogos detectados. */
    public static void clear(Context c) {
        prefs(c).edit().remove(KEY_GAMES).apply();
    }

    // ---------------------------------------------------------------- Varredura

    private static final Pattern PAT_TID = Pattern.compile(
            "(?i)(?:Title\\s*ID|Program\\s*ID|Application\\s*ID|TitleId|ProgramId)[\\s:=]+(?:0x)?([0-9a-f]{16})");
    private static final Pattern PAT_BID = Pattern.compile(
            "(?i)(?:Main\\s+NSO\\s+Build\\s+ID|Build\\s*ID|BuildId|NSO\\s*Build\\s*ID)[\\s:=]+([0-9a-f]{16})");
    private static final Pattern PAT_VER = Pattern.compile(
            "(?i)(?:Display\\s*Version|Application\\s*Version|Patch\\s*Version|Version)[\\s:=]+([vV]?[0-9]+(?:\\.[0-9]+)*)");
    private static final Pattern PAT_NAME = Pattern.compile(
            "(?i)(?:Application\\s*Title|Application\\s*Name|Title\\s*Name)[\\s:=]+([^\\r\\n]+)");
    private static final Pattern PAT_BRACKET_TID = Pattern.compile("\\[(0100[0-9A-Fa-f]{12})\\]");
    private static final Pattern PAT_BRACKET_VER = Pattern.compile("\\[[vV]?([0-9]+(?:\\.[0-9]+)*)\\]");

    private static void record(Map<String, ScannedGame> map, String tid, String bid, String name, String ver, String src) {
        if (tid == null || !Util.isHex16(tid)) return;
        String t = tid.toUpperCase(Locale.US);
        recordSingle(map, t, bid, name, ver, src);
        String b = baseTid(t);
        if (!b.equals(t)) {
            recordSingle(map, b, bid, name, ver, src);
        }
    }

    private static void recordSingle(Map<String, ScannedGame> map, String t, String bid, String name, String ver, String src) {
        ScannedGame g = map.get(t);
        if (g == null) {
            g = new ScannedGame(t, bid, name, ver, src);
            map.put(t, g);
        } else {
            if (bid != null && Util.isHex16(bid)) g.bid = bid.toUpperCase(Locale.US);
            if (name != null && !name.trim().isEmpty()) g.title = name.trim();
            if (ver != null && !ver.trim().isEmpty()) g.version = ver.trim();
            if (src != null) g.source = src;
            g.lastSeen = System.currentTimeMillis();
        }
    }

    /** Lê e extrai Title IDs, Build IDs e versões de um stream de log do Eden/Yuzu. */
    private static void parseLogStream(InputStream in, Map<String, ScannedGame> games) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String curTid = null;
            String curBid = null;
            String curName = null;
            String curVer = null;

            String line;
            while ((line = reader.readLine()) != null) {
                // Identificação de Title ID
                Matcher mTid = PAT_TID.matcher(line);
                if (mTid.find()) {
                    String tid = mTid.group(1).toUpperCase(Locale.US);
                    if (!tid.equals(curTid)) {
                        if (curTid != null) {
                            record(games, curTid, curBid, curName, curVer, "log");
                        }
                        curTid = tid;
                        curBid = null;
                        curName = null;
                        curVer = null;
                    }
                } else {
                    Matcher mBr = PAT_BRACKET_TID.matcher(line);
                    if (mBr.find()) {
                        String tid = mBr.group(1).toUpperCase(Locale.US);
                        if (!tid.equals(curTid)) {
                            if (curTid != null) {
                                record(games, curTid, curBid, curName, curVer, "log");
                            }
                            curTid = tid;
                            curBid = null;
                            curName = null;
                            curVer = null;
                        }
                    }
                }

                // Identificação de Build ID
                Matcher mBid = PAT_BID.matcher(line);
                if (mBid.find()) {
                    curBid = mBid.group(1).toUpperCase(Locale.US);
                    if (curTid != null) {
                        record(games, curTid, curBid, curName, curVer, "log");
                    }
                }

                // Identificação de Nome do Jogo
                Matcher mName = PAT_NAME.matcher(line);
                if (mName.find()) {
                    curName = mName.group(1).trim();
                    if (curTid != null) {
                        record(games, curTid, curBid, curName, curVer, "log");
                    }
                }

                // Identificação de Versão
                Matcher mVer = PAT_VER.matcher(line);
                if (mVer.find()) {
                    curVer = mVer.group(1).trim();
                    if (curTid != null) {
                        record(games, curTid, curBid, curName, curVer, "log");
                    }
                } else {
                    Matcher mBrVer = PAT_BRACKET_VER.matcher(line);
                    if (mBrVer.find()) {
                        curVer = mBrVer.group(1).trim();
                        if (curTid != null) {
                            record(games, curTid, curBid, curName, curVer, "log");
                        }
                    }
                }
            }
            if (curTid != null) {
                record(games, curTid, curBid, curName, curVer, "log");
            }
        }
    }

    private static void notifyProgress(Util.Progress p, String msg) {
        if (p != null) p.onProgress(msg);
    }

    /**
     * Executa a varredura profunda no diretório SAF do emulador Eden.
     * Retorna o total de jogos identificados.
     */
    public static int scanEden(Context c, Util.Progress p) throws Exception {
        Uri tree = SafStore.getTree(c);
        if (tree == null) {
            throw new IOException("Nenhuma pasta do Eden selecionada. Configure a pasta primeiro.");
        }

        ContentResolver cr = c.getContentResolver();
        Map<String, ScannedGame> games = new LinkedHashMap<>(getScannedGames(c));

        Uri root = SafStore.rootDoc(tree);
        String rootName = SafStore.displayName(cr, root);
        Uri base = root;
        if (!"files".equalsIgnoreCase(rootName)) {
            Uri filesChild = SafStore.find(cr, tree, root, "files");
            if (filesChild != null) base = filesChild;
        }

        // 1. Procurar arquivos de log (yuzu_log.txt, eden_log.txt, log.txt, pasta log/)
        notifyProgress(p, "Analisando registros de inicialização do Eden…");
        scanLogs(cr, tree, base, root, games);

        // 2. Procurar na pasta load/ (cheats e mods já organizados por TitleID e BuildID)
        notifyProgress(p, "Verificando pastas de mods e cheats (load/)…");
        scanLoad(cr, tree, base, root, games);

        // 3. Procurar em config/custom/ (configurações individuais de jogos .ini)
        notifyProgress(p, "Verificando perfis individuais de jogos (config/)…");
        scanConfig(cr, tree, base, root, games);

        // 4. Procurar em nand/user/save/ (saves de jogos executados no emulador)
        notifyProgress(p, "Verificando dados salvos de jogos (nand/)…");
        scanSaves(cr, tree, base, root, games);

        // 5. Completar nomes a partir do banco de títulos (Names)
        Names.ensure(c);
        for (ScannedGame g : games.values()) {
            if (g.title == null || g.title.trim().isEmpty()) {
                String known = Names.get(g.tid);
                if (known != null && !known.trim().isEmpty()) {
                    g.title = known;
                }
            }
        }

        // Salvar tudo no cache persistente
        saveScannedGames(c, games);
        return games.size();
    }

    private static void scanLogs(ContentResolver cr, Uri tree, Uri base, Uri root, Map<String, ScannedGame> games) {
        String[] logNames = new String[]{"yuzu_log.txt", "eden_log.txt", "log.txt", "yuzu_log.txt.old.txt"};
        for (String name : logNames) {
            Uri f = SafStore.find(cr, tree, base, name);
            if (f == null && base != root) f = SafStore.find(cr, tree, root, name);
            if (f != null) {
                try (InputStream in = cr.openInputStream(f)) {
                    if (in != null) parseLogStream(in, games);
                } catch (Exception ignored) {}
            }
        }

        // Pasta log/
        Uri logDir = SafStore.find(cr, tree, base, "log");
        if (logDir == null && base != root) logDir = SafStore.find(cr, tree, root, "log");
        if (logDir != null) {
            for (SafStore.Entry e : SafStore.list(cr, tree, logDir)) {
                if (!e.dir && e.name.toLowerCase(Locale.US).endsWith(".txt")) {
                    try (InputStream in = cr.openInputStream(SafStore.doc(tree, e.docId))) {
                        if (in != null) parseLogStream(in, games);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static void scanLoad(ContentResolver cr, Uri tree, Uri base, Uri root, Map<String, ScannedGame> games) {
        Uri load = SafStore.find(cr, tree, base, "load");
        if (load == null && base != root) load = SafStore.find(cr, tree, root, "load");
        if (load == null) return;

        for (SafStore.Entry tidEntry : SafStore.list(cr, tree, load)) {
            if (!tidEntry.dir) continue;
            String tid = tidEntry.name.toUpperCase(Locale.US);
            if (!tid.matches("0100[0-9A-F]{12}")) continue;

            record(games, tid, null, null, null, "load");

            Uri tidDoc = SafStore.doc(tree, tidEntry.docId);
            for (SafStore.Entry modEntry : SafStore.list(cr, tree, tidDoc)) {
                if (!modEntry.dir) continue;
                Uri modDoc = SafStore.doc(tree, modEntry.docId);
                Uri cheatsDir = SafStore.find(cr, tree, modDoc, "cheats");
                if (cheatsDir != null) {
                    for (SafStore.Entry cheatFile : SafStore.list(cr, tree, cheatsDir)) {
                        if (cheatFile.dir) continue;
                        String fn = cheatFile.name;
                        if (fn.toLowerCase(Locale.US).endsWith(".txt")) {
                            String bid = fn.substring(0, fn.length() - 4).toUpperCase(Locale.US);
                            if (Util.isHex16(bid)) {
                                record(games, tid, bid, null, null, "load");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void scanConfig(ContentResolver cr, Uri tree, Uri base, Uri root, Map<String, ScannedGame> games) {
        Uri config = SafStore.find(cr, tree, base, "config");
        if (config == null && base != root) config = SafStore.find(cr, tree, root, "config");
        Uri custom = null;
        if (config != null) custom = SafStore.find(cr, tree, config, "custom");
        if (custom == null) custom = SafStore.find(cr, tree, base, "custom");
        if (custom == null) return;

        for (SafStore.Entry iniEntry : SafStore.list(cr, tree, custom)) {
            if (iniEntry.dir) continue;
            String name = iniEntry.name;
            if (name.toLowerCase(Locale.US).endsWith(".ini")) {
                String tid = name.substring(0, name.length() - 4).toUpperCase(Locale.US);
                if (tid.matches("0100[0-9A-F]{12}")) {
                    String title = null;
                    String ver = null;
                    try (InputStream in = cr.openInputStream(SafStore.doc(tree, iniEntry.docId))) {
                        if (in != null) {
                            byte[] b = Util.readAll(in);
                            String text = new String(b, StandardCharsets.UTF_8);
                            for (String l : text.split("\n")) {
                                l = l.trim();
                                int eq = l.indexOf('=');
                                if (eq > 0) {
                                    String k = l.substring(0, eq).trim().toLowerCase(Locale.US);
                                    String v = l.substring(eq + 1).trim();
                                    if (k.equals("title_name") && !v.isEmpty()) title = v;
                                    else if (k.equals("version") && !v.isEmpty()) ver = v;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    record(games, tid, null, title, ver, "config");
                }
            }
        }
    }

    private static void scanSaves(ContentResolver cr, Uri tree, Uri base, Uri root, Map<String, ScannedGame> games) {
        Uri nand = SafStore.find(cr, tree, base, "nand");
        if (nand == null && base != root) nand = SafStore.find(cr, tree, root, "nand");
        if (nand == null) return;

        Uri user = SafStore.find(cr, tree, nand, "user");
        if (user == null) return;
        Uri save = SafStore.find(cr, tree, user, "save");
        if (save == null) return;

        for (SafStore.Entry sys : SafStore.list(cr, tree, save)) {
            if (!sys.dir) continue;
            Uri sysDoc = SafStore.doc(tree, sys.docId);
            for (SafStore.Entry prof : SafStore.list(cr, tree, sysDoc)) {
                if (!prof.dir) continue;
                Uri profDoc = SafStore.doc(tree, prof.docId);
                for (SafStore.Entry g : SafStore.list(cr, tree, profDoc)) {
                    if (!g.dir) continue;
                    String tid = g.name.toUpperCase(Locale.US);
                    if (tid.matches("0100[0-9A-F]{12}")) {
                        record(games, tid, null, null, null, "save");
                    }
                }
            }
        }
    }

    /** Permite varrer uma pasta de jogos/ROMs (.nsp, .xci, .nsz) escolhida pelo usuário. */
    public static int scanRomsFolder(Context c, Uri romsTree, Util.Progress p) throws Exception {
        ContentResolver cr = c.getContentResolver();
        Map<String, ScannedGame> games = new LinkedHashMap<>(getScannedGames(c));
        Uri root = SafStore.rootDoc(romsTree);

        notifyProgress(p, "Identificando arquivos de jogos (.nsp, .xci)…");
        for (SafStore.Entry e : SafStore.list(cr, romsTree, root)) {
            if (e.dir) continue;
            String name = e.name;
            String lower = name.toLowerCase(Locale.US);
            if (lower.endsWith(".nsp") || lower.endsWith(".xci") || lower.endsWith(".nsz") || lower.endsWith(".xcz")) {
                Matcher mTid = PAT_BRACKET_TID.matcher(name);
                if (mTid.find()) {
                    String tid = mTid.group(1).toUpperCase(Locale.US);
                    String ver = null;
                    Matcher mVer = PAT_BRACKET_VER.matcher(name);
                    if (mVer.find()) ver = mVer.group(1);
                    record(games, tid, null, null, ver, "rom");
                }
            }
        }
        Names.ensure(c);
        for (ScannedGame g : games.values()) {
            if (g.title == null || g.title.trim().isEmpty()) {
                String known = Names.get(g.tid);
                if (known != null && !known.trim().isEmpty()) g.title = known;
            }
        }
        saveScannedGames(c, games);
        return games.size();
    }
}
