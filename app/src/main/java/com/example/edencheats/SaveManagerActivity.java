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

        // Cabeçalho com botão voltar
        root.addView(Ui.headerCard(this, Lang.s(R.string.tools_sm_title), Lang.s(R.string.tools_sm_sub), Ui.MUTED), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Ações Rápidas de Saves
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        Button backupAllBtn = Ui.button(this, Lang.s(R.string.tools_sm_backup_all), true, v -> backupAll());
        Button restoreBtn = Ui.button(this, Lang.s(R.string.tools_sm_restore_zip), false, v -> pickRestoreZip(null));
        actionsRow.addView(backupAllBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        actionsRow.addView(restoreBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(actionsRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        search = Ui.monoEdit(this, Lang.s(R.string.tools_sm_search_hint));
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
        root.addView(Ui.searchBox(this, search), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 3));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        Ui.styleList(list);
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
            status.setText(Lang.s(R.string.tools_sm_select_folder));
            return;
        }
        status.setText(Lang.s(R.string.tools_sm_locating));
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
                if (error != null) status.setText(Lang.s(R.string.tools_sm_read_error, error));
                else if (allSaves.isEmpty()) {
                    status.setText(Lang.s(R.string.tools_sm_none_found));
                } else {
                    status.setText(Lang.s(R.string.tools_sm_detected, allSaves.size()));
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
            String sub = Lang.s(R.string.tools_sm_items, gs.tid, gs.fileCount);
            adapter.rows.add(new String[]{"💾 " + gs.name, sub, "0"});
        }
        adapter.notifyDataSetChanged();
    }

    private void showSaveOptions(final GameSave gs) {
        String[] options = new String[]{
                Lang.s(R.string.tools_sm_opt_backup),
                Lang.s(R.string.tools_sm_opt_restore),
                Lang.s(R.string.tools_sm_opt_delete)
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
        status.setText(Lang.s(R.string.tools_sm_zipping, gs.name));
        Util.POOL.execute(() -> {
            try {
                Uri tree = SafStore.getTree(this);
                ContentResolver cr = getContentResolver();
                Uri saveFolder = SafStore.doc(tree, gs.saveDocId);
                Map<String, byte[]> files = new LinkedHashMap<>();
                SafStore.collectFiles(cr, tree, saveFolder, "", files);
                if (files.isEmpty()) {
                    runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.tools_sm_folder_empty)));
                    return;
                }
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String cleanName = Util.sanitize(gs.name).replaceAll("\\s+", "_");
                String zipName = cleanName + "_" + gs.tid + "_Save_" + stamp + ".zip";
                runOnUiThread(() -> {
                    exportZip(zipName, files);
                    Ui.toast(this, Lang.s(R.string.tools_sm_pick_dest));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.tools_sm_backup_error, e.getMessage())));
            }
        });
    }

    private void backupAll() {
        if (allSaves.isEmpty()) {
            Ui.toast(this, Lang.s(R.string.tools_sm_nothing_export));
            return;
        }
        status.setText(Lang.s(R.string.tools_sm_packing));
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
                    runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.tools_sm_no_files)));
                    return;
                }
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String zipName = "Eden_Backup_Todos_Saves_" + stamp + ".zip";
                runOnUiThread(() -> {
                    exportZip(zipName, allFiles);
                    Ui.toast(this, Lang.s(R.string.tools_sm_pick_dest_all));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.tools_sm_backup_all_error, e.getMessage())));
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
                .setTitle(Lang.s(R.string.tools_sm_del_title))
                .setMessage(Lang.s(R.string.tools_sm_del_msg, gs.name, gs.tid))
                .setPositiveButton(Lang.s(R.string.tools_sm_del_yes), (d, w) -> Util.POOL.execute(() -> {
                    try {
                        SafStore.delete(this, SafStore.getTree(this), gs.saveDocId);
                        runOnUiThread(() -> {
                            Ui.toast(this, Lang.s(R.string.tools_sm_deleted));
                            refresh();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.tools_sm_del_error, e.getMessage())));
                    }
                }))
                .setNegativeButton(Lang.s(R.string.tools_cancel), null)
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
        status.setText(Lang.s(R.string.tools_sm_restoring));
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
                    Ui.toast(this, Lang.s(R.string.tools_sm_restored, total));
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText(Lang.s(R.string.tools_sm_restore_error, e.getMessage()));
                    Ui.toast(this, Lang.s(R.string.tools_sm_error, e.getMessage()));
                });
            }
        });
    }
}
