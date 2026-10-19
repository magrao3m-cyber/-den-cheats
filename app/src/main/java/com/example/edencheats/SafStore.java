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
        if (tree == null) return "Nenhuma pasta selecionada.";
        try {
            a.getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            return "O Android recusou essa pasta: " + e.getMessage();
        }
        prefs(a).edit().putString("tree", tree.toString()).apply();
        String name = "?";
        try {
            name = displayName(a.getContentResolver(), rootDoc(tree));
        } catch (Exception ignored) {
            // só informativo
        }
        return "Pasta de destino: " + name;
    }

    static String describe(Context c) {
        Uri t = getTree(c);
        if (t == null) return "(nenhuma pasta escolhida)";
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
            if (name.equals(e.name)) return doc(tree, e.docId);
        }
        return null;
    }

    static Uri ensureDir(ContentResolver cr, Uri tree, Uri parent, String name) throws FileNotFoundException {
        Uri d = find(cr, tree, parent, name);
        if (d != null) return d;
        Uri n = DocumentsContract.createDocument(cr, parent, Document.MIME_TYPE_DIR, name);
        if (n == null) throw new FileNotFoundException("Não foi possível criar a pasta " + name);
        return n;
    }

    static void writeFile(ContentResolver cr, Uri tree, Uri dir, String name, byte[] data) throws IOException {
        Uri f = find(cr, tree, dir, name);
        if (f == null) {
            f = DocumentsContract.createDocument(cr, dir, "application/octet-stream", name);
        }
        if (f == null) throw new FileNotFoundException("Não foi possível criar " + name);
        try (OutputStream o = cr.openOutputStream(f, "wt")) {
            if (o == null) throw new IOException("Sem acesso de escrita a " + name);
            o.write(data);
        }
    }

    /** Devolve a pasta "load" (cria se necessário). Se a pasta escolhida já for "load" ou contiver "files", adapta. */
    static Uri loadDir(Context c, boolean create) throws IOException {
        Uri tree = getTree(c);
        if (tree == null) throw new IOException("Nenhuma pasta de destino configurada.");
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
        if (tree == null) throw new IOException("Nenhuma pasta de destino configurada.");
        ContentResolver cr = c.getContentResolver();
        Uri load = loadDir(c, true);
        Uri tidDir = ensureDir(cr, tree, load, tid);
        int n = 0;
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String[] parts = e.getKey().split("/");
            Uri dir = tidDir;
            for (int i = 0; i < parts.length - 1; i++) {
                dir = ensureDir(cr, tree, dir, parts[i]);
            }
            writeFile(cr, tree, dir, parts[parts.length - 1], e.getValue());
            n++;
        }
        return n + " arquivo(s) gravado(s) em load/" + tid + "/";
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
            throw new IOException("O Android não permitiu apagar.");
        }
    }
}
