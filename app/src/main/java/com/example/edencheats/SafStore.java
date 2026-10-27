package com.example.edencheats;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Acesso à pasta de destino via Storage Access Framework (sem permissões especiais).
 * A estrutura criada é: [pasta escolhida]/load/[TitleID]/[NomeDoMod]/(cheats|exefs|romfs)/...
 */
final class SafStore {
    private SafStore() {}

    static final String DEFAULT_PKG = "dev.eden.eden_emulator";
    private static final String PREFS = "eden_cheats";

    static final class Entry {
        final String docId;
        final String name;
        final boolean dir;

        Entry(String docId, String name, boolean dir) {
            this.docId = docId;
            this.name = name;
            this.dir = dir;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String pkg(Context c) {
        String p = prefs(c).getString("pkg", DEFAULT_PKG);
        return (p == null || p.trim().isEmpty()) ? DEFAULT_PKG : p.trim();
    }

    static void setPkg(Context c, String pkg) {
        prefs(c).edit().putString("pkg", pkg == null ? "" : pkg.trim()).apply();
    }

    /** Pasta configurada, ou null se não houver / a permissão foi perdida. */
    static Uri getTree(Context c) {
        String s = prefs(c).getString("tree", null);
        if (s == null) return null;
        Uri u = Uri.parse(s);
        for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().equals(u) && p.isWritePermission()) return u;
        }
        return null;
    }

    static void pick(Activity a, int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 26) {
            Uri init = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2F"
                    + pkg(a));
            i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, init);
        }
        a.startActivityForResult(i, req);
    }

    /** Processa o resultado do seletor de pasta; devolve uma mensagem para o usuário. */
    static String onPicked(Activity a, Intent data) {
        Uri tree = data.getData();
        if (tree == null) return Lang.s(R.string.core_saf_none_selected);
        try {
            a.getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            return Lang.s(R.string.core_saf_refused, e.getMessage());
        }
        prefs(a).edit().putString("tree", tree.toString()).apply();
        String name = "?";
        try {
            name = displayName(a.getContentResolver(), rootDoc(tree));
        } catch (Exception ignored) {
            // só informativo
        }
        return Lang.s(R.string.core_saf_dest, name);
    }

    static String describe(Context c) {
        Uri t = getTree(c);
        if (t == null) return Lang.s(R.string.core_saf_none_chosen);
        try {
            return displayName(c.getContentResolver(), rootDoc(t)) + "  →  load/<TitleID>/<Mod>/";
        } catch (Exception e) {
            return t.getLastPathSegment() + "  →  load/<TitleID>/<Mod>/";
        }
    }

    // ------------------------------------------------------------ helpers SAF

    static Uri rootDoc(Uri tree) {
        return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    }

    static Uri doc(Uri tree, String docId) {
        return DocumentsContract.buildDocumentUriUsingTree(tree, docId);
    }

    static String displayName(ContentResolver cr, Uri docUri) {
        try (Cursor cur = cr.query(docUri, new String[]{Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) return cur.getString(0);
        }
        return "";
    }

    static List<Entry> list(ContentResolver cr, Uri tree, Uri parent) {
        List<Entry> out = new ArrayList<>();
        Uri kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent));
        try (Cursor cur = cr.query(kids, new String[]{
                Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE}, null, null, null)) {
            if (cur != null) {
                while (cur.moveToNext()) {
                    out.add(new Entry(cur.getString(0), cur.getString(1),
                            Document.MIME_TYPE_DIR.equals(cur.getString(2))));
                }
            }
        }
        return out;
    }

    static Uri find(ContentResolver cr, Uri tree, Uri parent, String name) {
        for (Entry e : list(cr, tree, parent)) {
            if (name.equalsIgnoreCase(e.name)) return doc(tree, e.docId);
        }
        return null;
    }

    static Uri ensureDir(ContentResolver cr, Uri tree, Uri parent, String name) throws FileNotFoundException {
        Uri d = find(cr, tree, parent, name);
        if (d != null) return d;
        Uri n = DocumentsContract.createDocument(cr, parent, Document.MIME_TYPE_DIR, name);
        if (n == null) throw new FileNotFoundException(Lang.s(R.string.core_saf_mkdir_fail, name));
        return n;
    }

    static void writeFile(ContentResolver cr, Uri tree, Uri dir, String name, byte[] data) throws IOException {
        Uri f = find(cr, tree, dir, name);
        if (f == null) {
            f = DocumentsContract.createDocument(cr, dir, "application/octet-stream", name);
        }
        if (f == null) throw new FileNotFoundException(Lang.s(R.string.core_saf_create_fail, name));
        try (OutputStream o = cr.openOutputStream(f, "wt")) {
            if (o == null) throw new IOException(Lang.s(R.string.core_saf_no_write, name));
            o.write(data);
        }
    }

    /** Devolve a pasta "load" (cria se necessário). Se a pasta escolhida já for "load" ou contiver "files", adapta. */
    static Uri loadDir(Context c, boolean create) throws IOException {
        Uri tree = getTree(c);
        if (tree == null) throw new IOException(Lang.s(R.string.core_saf_no_dest));
        ContentResolver cr = c.getContentResolver();
        Uri root = rootDoc(tree);
        String rootName = displayName(cr, root);
        if ("load".equalsIgnoreCase(rootName)) return root;

        Uri base = root;
        // Se a pasta escolhida for o pacote do app (ex: dev.eden.eden_emulator),
        // ou se contiver a pasta "files", o Eden espera os arquivos dentro de files/load
        if (!"files".equalsIgnoreCase(rootName)) {
            Uri filesDir = find(cr, tree, root, "files");
            if (filesDir != null) {
                base = filesDir;
            } else if (rootName.equalsIgnoreCase(DEFAULT_PKG) || rootName.equalsIgnoreCase(pkg(c))) {
                if (create) base = ensureDir(cr, tree, root, "files");
            }
        }

        if (create) return ensureDir(cr, tree, base, "load");
        return find(cr, tree, base, "load");
    }

    /** Grava arquivos em load/TID/. As chaves de 'files' são caminhos relativos a load/TID/ (ex: Mod/cheats/BID.txt). */
    static String install(Context c, String tid, Map<String, byte[]> files) throws IOException {
        Uri tree = getTree(c);
        if (tree == null) throw new IOException(Lang.s(R.string.core_saf_no_dest));
        ContentResolver cr = c.getContentResolver();
        Uri load = loadDir(c, true);
        Uri tidDir = ensureDir(cr, tree, load, tid);
        int n = 0;
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String key = e.getKey();
            if (key.contains("..") || key.startsWith("/") || key.startsWith("\\")) {
                continue; // Rejeita caminhos inseguros
            }
            String[] parts = key.split("/");
            Uri dir = tidDir;
            for (int i = 0; i < parts.length - 1; i++) {
                String p = parts[i].trim();
                if (p.isEmpty() || p.equals(".") || p.equals("..")) continue;
                dir = ensureDir(cr, tree, dir, p);
            }
            String fn = parts[parts.length - 1].trim();
            if (fn.isEmpty() || fn.contains("..")) continue;
            writeFile(cr, tree, dir, fn, e.getValue());
            // Confere se o arquivo realmente existe depois de gravar (o Android às vezes aceita e não grava).
            if (find(cr, tree, dir, fn) == null) {
                throw new IOException(Lang.s(R.string.ux_verify_failed, key));
            }
            n++;
        }
        return Lang.s(R.string.core_saf_installed, n, tid);
    }

    /** Nome da pasta escolhida pelo usuário (para mostrar onde os cheats foram gravados). */
    static String chosenName(Context c) {
        try {
            Uri tree = getTree(c);
            if (tree == null) return "?";
            String n = displayName(c.getContentResolver(), rootDoc(tree));
            return n.isEmpty() ? "?" : n;
        } catch (Exception e) {
            return "?";
        }
    }

    /** A pasta escolhida parece ser a pasta de dados do Eden (files / load / nand / config / keys...)? */
    static boolean looksLikeEdenData(Context c) {
        try {
            Uri tree = getTree(c);
            if (tree == null) return false;
            ContentResolver cr = c.getContentResolver();
            Uri root = rootDoc(tree);
            String name = displayName(cr, root);
            if ("files".equalsIgnoreCase(name) || "load".equalsIgnoreCase(name)) return true;
            for (Entry e : list(cr, tree, root)) {
                String n = e.name.toLowerCase(java.util.Locale.ROOT);
                if (e.dir && (n.equals("files") || n.equals("nand") || n.equals("config")
                        || n.equals("keys") || n.equals("load") || n.equals("shader"))) return true;
            }
            return false;
        } catch (Exception e) {
            return true; // na dúvida, não assusta o usuário
        }
    }

    /** Texto que diz ONDE os cheats foram gravados e avisa se a pasta não parece ser a do Eden. */
    static String destinationNote(Context c, String tid) {
        String note = Lang.s(R.string.ux_dest_note, chosenName(c), tid);
        if (!looksLikeEdenData(c)) {
            note += "\n\n" + Lang.s(R.string.ux_dest_warn, chosenName(c), pkg(c));
        }
        return note;
    }

    /** Compatibilidade para chamadas que especificam um único mod: grava em load/TID/mod/. */
    static String install(Context c, String tid, String mod, Map<String, byte[]> files) throws IOException {
        java.util.Map<String, byte[]> mapped = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            mapped.put(mod + "/" + e.getKey(), e.getValue());
        }
        return install(c, tid, mapped);
    }

    static void delete(Context c, Uri tree, String docId) throws IOException {
        if (!DocumentsContract.deleteDocument(c.getContentResolver(), doc(tree, docId))) {
            throw new IOException(Lang.s(R.string.core_saf_delete_denied));
        }
    }

    static Uri rename(ContentResolver cr, Uri docUri, String newName) throws FileNotFoundException {
        return DocumentsContract.renameDocument(cr, docUri, newName);
    }

    /** Coleta todos os arquivos de um diretório recursivamente como mapa de caminhos relativos para bytes. */
    static void collectFiles(ContentResolver cr, Uri tree, Uri dir, String prefix, Map<String, byte[]> out) throws IOException {
        for (Entry e : list(cr, tree, dir)) {
            Uri itemUri = doc(tree, e.docId);
            String path = prefix.isEmpty() ? e.name : prefix + "/" + e.name;
            if (e.dir) {
                collectFiles(cr, tree, itemUri, path, out);
            } else {
                try (java.io.InputStream in = cr.openInputStream(itemUri)) {
                    if (in != null) out.put(path, Util.readAll(in));
                }
            }
        }
    }

    /** Retorna a pasta base files/ ou raiz do emulador. */
    static Uri baseDir(Context c) {
        Uri tree = getTree(c);
        if (tree == null) return null;
        ContentResolver cr = c.getContentResolver();
        Uri root = rootDoc(tree);
        String rootName = displayName(cr, root);
        if ("files".equalsIgnoreCase(rootName)) return root;
        Uri filesChild = find(cr, tree, root, "files");
        return filesChild != null ? filesChild : root;
    }

    // ---- Leitura / remoção de cheats já instalados (desfazer, remover) ----

    /** Pasta load/TID, ou null se ainda não existir. */
    static Uri tidDir(Context c, String tid) throws IOException {
        Uri tree = getTree(c);
        if (tree == null) return null;
        Uri load = loadDir(c, false);
        if (load == null) return null;
        return find(c.getContentResolver(), tree, load, tid);
    }

    /** Pastas de load/TID que contêm uma subpasta "cheats" (um mod de cheat cada). */
    static List<Entry> cheatFolders(Context c, String tid) throws IOException {
        List<Entry> out = new ArrayList<>();
        Uri tree = getTree(c);
        Uri td = tidDir(c, tid);
        if (tree == null || td == null) return out;
        ContentResolver cr = c.getContentResolver();
        for (Entry e : list(cr, tree, td)) {
            if (e.dir && find(cr, tree, doc(tree, e.docId), "cheats") != null) out.add(e);
        }
        return out;
    }

    /** Lê os arquivos (chaves relativas a load/TID); o valor é null quando o arquivo não existe. */
    static Map<String, byte[]> readFiles(Context c, String tid, java.util.Collection<String> keys) {
        Map<String, byte[]> out = new java.util.LinkedHashMap<>();
        try {
            Uri tree = getTree(c);
            Uri td = tidDir(c, tid);
            ContentResolver cr = c.getContentResolver();
            for (String key : keys) {
                Uri cur = td;
                for (String part : key.split("/")) {
                    if (cur == null) break;
                    cur = find(cr, tree, cur, part);
                }
                byte[] data = null;
                if (cur != null) {
                    try (java.io.InputStream in = cr.openInputStream(cur)) {
                        if (in != null) data = Util.readAll(in);
                    }
                }
                out.put(key, data);
            }
        } catch (Exception e) {
            for (String key : keys) if (!out.containsKey(key)) out.put(key, null);
        }
        return out;
    }

    /** Apaga os arquivos (e as pastas que ficarem vazias, até load/TID). Devolve quantos arquivos foram apagados. */
    static int removeFiles(Context c, String tid, java.util.Collection<String> keys) throws IOException {
        Uri tree = getTree(c);
        Uri td = tidDir(c, tid);
        if (tree == null || td == null) return 0;
        ContentResolver cr = c.getContentResolver();
        int n = 0;
        for (String key : keys) {
            String[] parts = key.split("/");
            List<Uri> chain = new ArrayList<>();
            Uri cur = td;
            chain.add(cur);
            for (String part : parts) {
                cur = find(cr, tree, cur, part);
                if (cur == null) break;
                chain.add(cur);
            }
            if (cur == null || chain.size() != parts.length + 1) continue;
            if (DocumentsContract.deleteDocument(cr, cur)) n++;
            for (int i = chain.size() - 2; i >= 1; i--) {
                Uri d = chain.get(i);
                if (!list(cr, tree, d).isEmpty()) break;
                DocumentsContract.deleteDocument(cr, d);
            }
        }
        return n;
    }
}
