package com.example.edencheats;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gerenciador de Saves do Eden:
 * Permite fazer backup dos saves de qualquer jogo em arquivo .ZIP,
 * restaurar saves de backups anteriores e exportar todos os saves com 1 clique.
 */
public class SaveManagerActivity extends BaseActivity {
    private static final int REQ_RESTORE_ZIP = 21;

    static final class GameSave {
        String tid;
        String name;
        String profileDocId;
        String saveDocId;
        int fileCount;
        long totalBytes;
    }

    private EditText search;
    private TextView status;
    private Ui.RowAdapter adapter;
    private final List<GameSave> allSaves = new ArrayList<>();
    private final List<GameSave> shown = new ArrayList<>();
    private GameSave pendingRestoreTarget;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);

        // Header Card
        LinearLayout headCard = new LinearLayout(this);
        headCard.setOrientation(LinearLayout.VERTICAL);
        headCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        headCard.setPadding(p, p, p, p);
        headCard.addView(Ui.text(this, "💾 GERENCIADOR DE SAVES", 19, Ui.TEXT, true));
        headCard.addView(Ui.text(this, "Faça backup e restaure os dados salvos do emulador Eden com segurança", 12, Ui.MUTED, false));
        root.addView(headCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Ações Rápidas de Saves
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        Button backupAllBtn = Ui.button(this, "📦 Backup de TODOS", true, v -> backupAll());
        Button restoreBtn = Ui.button(this, "📥 Restaurar de ZIP", false, v -> pickRestoreZip(null));
        actionsRow.addView(backupAllBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        actionsRow.addView(restoreBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(actionsRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        search = Ui.edit(this, "🔍 Filtrar jogo com save por nome ou TID...");
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                applyFilter();
            }
        });
        root.addView(search, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 3));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((pView, v, pos, id) -> {
            if (pos >= 0 && pos < shown.size()) {
                showSaveOptions(shown.get(pos));
            }
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));

        setContentView(root);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (SafStore.getTree(this) == null) {
            status.setText("Selecione a pasta do Eden para carregar os saves.");
            return;
        }
        status.setText("🔍 Localizando dados salvos em nand/user/save/…");
        Util.POOL.execute(() -> {
            final List<GameSave> found = new ArrayList<>();
            String err = null;
            try {
                Uri tree = SafStore.getTree(this);
                ContentResolver cr = getContentResolver();
                Uri base = SafStore.baseDir(this);
                if (base != null) {
                    Uri nand = SafStore.find(cr, tree, base, "nand");
                    if (nand != null) {
                        Uri user = SafStore.find(cr, tree, nand, "user");
                        if (user != null) {
                            Uri save = SafStore.find(cr, tree, user, "save");
                            if (save != null) {
                                for (SafStore.Entry sys : SafStore.list(cr, tree, save)) {
                                    if (!sys.dir) continue;
                                    Uri sysDoc = SafStore.doc(tree, sys.docId);
                                    for (SafStore.Entry prof : SafStore.list(cr, tree, sysDoc)) {
                                        if (!prof.dir) continue;
                                        Uri profDoc = SafStore.doc(tree, prof.docId);
                                        for (SafStore.Entry gameSaveEntry : SafStore.list(cr, tree, profDoc)) {
                                            if (!gameSaveEntry.dir) continue;
                                            String tid = gameSaveEntry.name.toUpperCase(Locale.US);
                                            if (!tid.matches("0100[0-9A-F]{12}")) continue;

                                            GameSave gs = new GameSave();
                                            gs.tid = tid;
                                            gs.profileDocId = prof.docId;
                                            gs.saveDocId = gameSaveEntry.docId;

                                            // Contar arquivos e calcular tamanho
                                            Uri gDoc = SafStore.doc(tree, gameSaveEntry.docId);
                                            int count = 0;
                                            long size = 0;
                                            for (SafStore.Entry fe : SafStore.list(cr, tree, gDoc)) {
                                                count++;
                                                // Se for diretório ou arquivo
                                            }
                                            gs.fileCount = count;
                                            gs.totalBytes = size;
                                            found.add(gs);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                err = e.getMessage();
            }

            Names.ensure(this);
            for (GameSave gs : found) {
                String known = Names.get(gs.tid);
                gs.name = (known != null && !known.isEmpty()) ? known : gs.tid;
            }

            final String error = err;
            runOnUiThread(() -> {
                allSaves.clear();
                allSaves.addAll(found);
                applyFilter();
                if (error != null) status.setText("Erro ao ler saves: " + error);
                else if (allSaves.isEmpty()) {
                    status.setText("Nenhum jogo com save encontrado em nand/user/save/.\n"
                            + "Inicie um jogo no Eden para que o savegame seja criado.");
                } else {
                    status.setText("⚡ " + allSaves.size() + " jogo(s) com dados salvos detectados no Eden.");
                }
            });
        });
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        adapter.rows.clear();
        for (GameSave gs : allSaves) {
            if (!q.isEmpty() && !gs.name.toLowerCase(Locale.ROOT).contains(q) && !gs.tid.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            shown.add(gs);
            String sub = gs.tid + " · " + gs.fileCount + " item(ns) de save";
            adapter.rows.add(new String[]{"💾 " + gs.name, sub, "0"});
        }
        adapter.notifyDataSetChanged();
    }

    private void showSaveOptions(final GameSave gs) {
        String[] options = new String[]{
                "💾 Fazer Backup deste Save (.ZIP)",
                "📥 Restaurar Save deste Jogo",
                "🗑️ Apagar Dados Salvos"
        };
        new AlertDialog.Builder(this)
                .setTitle(gs.name)
                .setItems(options, (d, which) -> {
                    if (which == 0) backupSingle(gs);
                    else if (which == 1) pickRestoreZip(gs);
                    else if (which == 2) confirmDeleteSave(gs);
                })
                .show();
    }

    private void backupSingle(final GameSave gs) {
        status.setText("Compactando save de " + gs.name + "…");
        Util.POOL.execute(() -> {
            try {
                Uri tree = SafStore.getTree(this);
                ContentResolver cr = getContentResolver();
                Uri saveFolder = SafStore.doc(tree, gs.saveDocId);
                Map<String, byte[]> files = new LinkedHashMap<>();
                SafStore.collectFiles(cr, tree, saveFolder, "", files);
                if (files.isEmpty()) {
                    runOnUiThread(() -> Ui.toast(this, "A pasta de save está vazia."));
                    return;
                }
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String cleanName = Util.sanitize(gs.name).replaceAll("\\s+", "_");
                String zipName = cleanName + "_" + gs.tid + "_Save_" + stamp + ".zip";
                runOnUiThread(() -> {
                    exportZip(zipName, files);
                    Ui.toast(this, "Escolha onde salvar o backup do save.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, "Erro no backup: " + e.getMessage()));
            }
        });
    }

    private void backupAll() {
        if (allSaves.isEmpty()) {
            Ui.toast(this, "Nenhum save para exportar.");
            return;
        }
        status.setText("Empacotando todos os saves do Eden…");
        Util.POOL.execute(() -> {
            try {
                Uri tree = SafStore.getTree(this);
                ContentResolver cr = getContentResolver();
                Map<String, byte[]> allFiles = new LinkedHashMap<>();
                for (GameSave gs : allSaves) {
                    Uri saveFolder = SafStore.doc(tree, gs.saveDocId);
                    SafStore.collectFiles(cr, tree, saveFolder, gs.tid, allFiles);
                }
                if (allFiles.isEmpty()) {
                    runOnUiThread(() -> Ui.toast(this, "Nenhum arquivo de save encontrado."));
                    return;
                }
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String zipName = "Eden_Backup_Todos_Saves_" + stamp + ".zip";
                runOnUiThread(() -> {
                    exportZip(zipName, allFiles);
                    Ui.toast(this, "Escolha onde salvar o backup completo.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, "Erro no backup geral: " + e.getMessage()));
            }
        });
    }

    private void pickRestoreZip(GameSave target) {
        pendingRestoreTarget = target;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        startActivityForResult(i, REQ_RESTORE_ZIP);
    }

    private void confirmDeleteSave(final GameSave gs) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Excluir Dados Salvos?")
                .setMessage("Tem certeza de que deseja apagar os dados salvos de \"" + gs.name + "\" (" + gs.tid + ") do Eden?\nEssa ação é irreversível a menos que você tenha um backup.")
                .setPositiveButton("Sim, Excluir", (d, w) -> Util.POOL.execute(() -> {
                    try {
                        SafStore.delete(this, SafStore.getTree(this), gs.saveDocId);
                        runOnUiThread(() -> {
                            Ui.toast(this, "Savegame apagado do Eden.");
                            refresh();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Ui.toast(this, "Erro ao apagar save: " + e.getMessage()));
                    }
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_RESTORE_ZIP && res == RESULT_OK && data != null && data.getData() != null) {
            restoreFromZip(data.getData(), pendingRestoreTarget);
            pendingRestoreTarget = null;
        }
    }

    private void restoreFromZip(final Uri zipUri, final GameSave target) {
        status.setText("Restaurando arquivos de save…");
        Util.POOL.execute(() -> {
            int restoredCount = 0;
            try {
                ContentResolver cr = getContentResolver();
                Uri tree = SafStore.getTree(this);
                try (InputStream in = cr.openInputStream(zipUri); ZipInputStream zis = new ZipInputStream(in)) {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        if (e.isDirectory()) continue;
                        byte[] data = Util.readAll(zis);
                        String entryName = e.getName().replace('\\', '/');
                        // Se o ZIP tem prefixo com o TitleID (ex: 0100A6301214E000/save.bin)
                        String targetTid;
                        String relPath;
                        int slash = entryName.indexOf('/');
                        if (slash > 0 && entryName.substring(0, slash).matches("0100[0-9A-Fa-f]{12}")) {
                            targetTid = entryName.substring(0, slash).toUpperCase(Locale.US);
                            relPath = entryName.substring(slash + 1);
                        } else if (target != null) {
                            targetTid = target.tid;
                            relPath = entryName;
                        } else {
                            continue; // Não sabe para qual jogo pertence
                        }

                        // Localizar a pasta do save desse jogo
                        GameSave dest = null;
                        for (GameSave gs : allSaves) {
                            if (gs.tid.equalsIgnoreCase(targetTid)) {
                                dest = gs;
                                break;
                            }
                        }
                        if (dest != null) {
                            Uri saveDir = SafStore.doc(tree, dest.saveDocId);
                            String[] parts = relPath.split("/");
                            Uri curDir = saveDir;
                            for (int pIdx = 0; pIdx < parts.length - 1; pIdx++) {
                                curDir = SafStore.ensureDir(cr, tree, curDir, parts[pIdx]);
                            }
                            SafStore.writeFile(cr, tree, curDir, parts[parts.length - 1], data);
                            restoredCount++;
                        }
                        zis.closeEntry();
                    }
                }
                final int total = restoredCount;
                runOnUiThread(() -> {
                    Ui.toast(this, "✔ " + total + " arquivo(s) de save restaurados com sucesso!");
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Erro na restauração: " + e.getMessage());
                    Ui.toast(this, "Erro: " + e.getMessage());
                });
            }
        });
    }
}
