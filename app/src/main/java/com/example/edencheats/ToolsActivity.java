package com.example.edencheats;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

/**
 * Ferramentas & Diagnóstico do Eden:
 * Verifica chaves prod.keys, status da pasta SAF, tamanho dos logs e realiza limpeza.
 */
public class ToolsActivity extends BaseActivity {
    private TextView folderStatus;
    private TextView keysStatus;
    private TextView logsStatus;
    private TextView scannerStatus;
    private TextView updateStatus;
    private TextView dbStatus;

    private Uri yuzuLogUri = null;
    private Uri edenLogUri = null;
    private long logBytes = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Ui.BG);

        LinearLayout root = Ui.page(this);
        sv.addView(root);

        // Cabeçalho com botão voltar
        root.addView(Ui.headerCard(this, Lang.s(R.string.tools_header_title), Lang.s(R.string.tools_header_sub), Ui.MUTED), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // 1. Card Pasta do Eden
        LinearLayout folderCard = cardSection(this, Lang.s(R.string.tools_sec_folder));
        folderStatus = Ui.text(this, Lang.s(R.string.tools_loading), 13, Ui.TEXT, false);
        folderCard.addView(folderStatus);
        Button changeFolderBtn = Ui.button(this, Lang.s(R.string.tools_change_folder), false, v -> folderDialog());
        folderCard.addView(changeFolderBtn, Ui.lp(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(folderCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 2. Card Chaves (prod.keys / title.keys)
        LinearLayout keysCard = cardSection(this, Lang.s(R.string.tools_sec_keys));
        keysStatus = Ui.text(this, Lang.s(R.string.tools_checking_keys), 13, Ui.TEXT, false);
        keysCard.addView(keysStatus);
        root.addView(keysCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 3. Card Logs de Execução & Limpeza
        LinearLayout logCard = cardSection(this, Lang.s(R.string.tools_sec_logs));
        logsStatus = Ui.text(this, Lang.s(R.string.tools_calc_logs), 13, Ui.TEXT, false);
        logCard.addView(logsStatus);
        Button cleanLogsBtn = Ui.button(this, Lang.s(R.string.tools_clear_logs_btn), true, v -> clearLogs());
        logCard.addView(cleanLogsBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(logCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 4. Card Varredura & Cache do App
        LinearLayout scannerCard = cardSection(this, Lang.s(R.string.tools_sec_scan));
        scannerStatus = Ui.text(this, Lang.s(R.string.tools_loading_info), 13, Ui.TEXT, false);
        scannerCard.addView(scannerStatus);
        LinearLayout cacheRow = new LinearLayout(this);
        cacheRow.setOrientation(LinearLayout.HORIZONTAL);
        Button clearCacheBtn = Ui.button(this, Lang.s(R.string.tools_clear_cache), false, v -> clearAppCache());
        Button copyReportBtn = Ui.button(this, Lang.s(R.string.tools_copy_report), false, v -> copyReport());
        cacheRow.addView(clearCacheBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        cacheRow.addView(copyReportBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        scannerCard.addView(cacheRow);
        root.addView(scannerCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 5. Card Tarefa de Atualização Autônoma
        LinearLayout updateCard = cardSection(this, Lang.s(R.string.tools_sec_update));
        dbStatus = Ui.text(this, Lang.s(R.string.ux3_db_counting), 13, Ui.TEXT, true);
        updateCard.addView(dbStatus);
        updateStatus = Ui.text(this, AutoUpdater.getLastUpdateInfo(this), 13, Ui.TEXT, false);
        updateStatus.setPadding(0, Ui.dp(this, 4), 0, 0);
        updateCard.addView(updateStatus);
        refreshDbStats();

        LinearLayout updateRow = new LinearLayout(this);
        updateRow.setOrientation(LinearLayout.HORIZONTAL);
        Button runUpdateBtn = Ui.button(this, Lang.s(R.string.tools_run_update), true, v -> executeUpdateTask());
        Button restoreDbBtn = Ui.button(this, Lang.s(R.string.tools_restore_factory), false, v -> restoreDatabase());
        updateRow.addView(runUpdateBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        updateRow.addView(restoreDbBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        updateCard.addView(updateRow);
        root.addView(updateCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 6. Card Apoie o Projeto (doações via PayPal)
        LinearLayout supportCard = cardSection(this, Lang.s(R.string.tools_sec_support));
        supportCard.addView(Ui.text(this,
                Lang.s(R.string.tools_support_text),
                13, Ui.TEXT, false));
        Button coffeeBtn = Ui.button(this, Lang.s(R.string.tools_coffee), true, v -> Util.openDonation(this));
        supportCard.addView(coffeeBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(supportCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 7. Card Idioma (Português / English / Español)
        LinearLayout langCard = cardSection(this, Lang.s(R.string.lang_card_title));
        langCard.addView(Ui.text(this, Lang.s(R.string.lang_card_current, Lang.currentName()),
                13, Ui.TEXT, false));
        Button langBtn = Ui.button(this, Lang.s(R.string.lang_card_button), false, v -> Lang.showPicker(this));
        langCard.addView(langBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(langCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        setContentView(sv);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onFolderChanged() {
        super.onFolderChanged();
        refresh();
    }

    private static LinearLayout cardSection(Context c, String title) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.shape(c, Ui.CARD, 16, 1, Ui.BORDER));
        int p = Ui.dp(c, 12);
        card.setPadding(p, p, p, p);

        TextView lbl = Ui.text(c, title, 11, Ui.MUTED, true);
        lbl.setTypeface(android.graphics.Typeface.MONOSPACE);
        int pb = Ui.dp(c, 6);
        lbl.setPadding(0, 0, 0, pb);
        card.addView(lbl);
        return card;
    }

    private void refresh() {
        folderStatus.setText(SafStore.describe(this));
        final int gameCount = EdenScanner.getCount(this);
        scannerStatus.setText(Lang.s(R.string.tools_scan_status, gameCount));
        if (updateStatus != null) updateStatus.setText(AutoUpdater.getLastUpdateInfo(this));

        Util.POOL.execute(() -> {
            String kStatus = Lang.s(R.string.tools_no_folder);
            String lStatus = Lang.s(R.string.tools_no_logs_access);
            yuzuLogUri = null;
            edenLogUri = null;
            logBytes = 0;

            Uri tree = SafStore.getTree(this);
            if (tree != null) {
                ContentResolver cr = getContentResolver();
                Uri base = SafStore.baseDir(this);
                if (base != null) {
                    // Checar keys
                    Uri keysDir = SafStore.find(cr, tree, base, "keys");
                    if (keysDir != null) {
                        Uri prod = SafStore.find(cr, tree, keysDir, "prod.keys");
                        Uri title = SafStore.find(cr, tree, keysDir, "title.keys");
                        if (prod != null) {
                            kStatus = Lang.s(R.string.tools_keys_prod_ok) + "\n";
                            if (title != null) kStatus += Lang.s(R.string.tools_keys_title_ok);
                            else kStatus += Lang.s(R.string.tools_keys_title_missing);
                        } else {
                            kStatus = Lang.s(R.string.tools_keys_prod_missing);
                        }
                    } else {
                        kStatus = Lang.s(R.string.tools_keys_dir_missing);
                    }

                    // Checar logs
                    String[] logNames = new String[]{"yuzu_log.txt", "eden_log.txt", "log.txt"};
                    for (String ln : logNames) {
                        Uri f = SafStore.find(cr, tree, base, ln);
                        if (f != null) {
                            if (ln.contains("yuzu")) yuzuLogUri = f;
                            if (ln.contains("eden")) edenLogUri = f;
                            try (Cursor c = cr.query(f, new String[]{DocumentsContract.Document.COLUMN_SIZE}, null, null, null)) {
                                if (c != null && c.moveToFirst()) {
                                    logBytes += c.getLong(0);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (logBytes > 0) {
                        lStatus = Lang.s(R.string.tools_logs_size, Util.mb(logBytes),
                                logBytes > 10 * 1048576L ? Lang.s(R.string.tools_logs_big) : Lang.s(R.string.tools_logs_normal));
                    } else {
                        lStatus = Lang.s(R.string.tools_no_log_files);
                    }
                }
            }

            final String finalK = kStatus;
            final String finalL = lStatus;
            runOnUiThread(() -> {
                keysStatus.setText(finalK);
                logsStatus.setText(finalL);
            });
        });
    }

    private void clearLogs() {
        if (SafStore.getTree(this) == null) {
            Ui.toast(this, Lang.s(R.string.tools_select_folder_first));
            return;
        }
        Util.POOL.execute(() -> {
            int cleared = 0;
            ContentResolver cr = getContentResolver();
            Uri[] targets = new Uri[]{yuzuLogUri, edenLogUri};
            for (Uri u : targets) {
                if (u != null) {
                    try (OutputStream o = cr.openOutputStream(u, "wt")) {
                        if (o != null) {
                            o.write(new byte[0]); // Trunca o log para 0 bytes
                            cleared++;
                        }
                    } catch (Exception ignored) {}
                }
            }
            final int c = cleared;
            runOnUiThread(() -> {
                if (c > 0) Ui.toast(this, Lang.s(R.string.tools_logs_cleared));
                else Ui.toast(this, Lang.s(R.string.tools_no_logs_to_clear));
                refresh();
            });
        });
    }

    private void clearAppCache() {
        Util.POOL.execute(() -> {
            long freed = 0;
            File[] cacheDirs = new File[]{getCacheDir(), getFilesDir()};
            for (File d : cacheDirs) {
                if (d != null && d.isDirectory()) {
                    for (File f : d.listFiles()) {
                        if (f != null && f.getName().endsWith(".zip") && !f.getName().contains("ibnux")) {
                            freed += f.length();
                            f.delete();
                        }
                    }
                }
            }
            final long f = freed;
            runOnUiThread(() -> {
                Ui.toast(this, Lang.s(R.string.tools_cache_cleared, Util.mb(f)));
                refresh();
            });
        });
    }

    private void copyReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(Lang.s(R.string.tools_rep_header) + "\n");
        sb.append(Lang.s(R.string.tools_rep_folder) + ": ").append(SafStore.describe(this)).append("\n");
        sb.append(Lang.s(R.string.tools_rep_keys) + ": ").append(keysStatus.getText()).append("\n");
        sb.append(Lang.s(R.string.tools_rep_logs) + ": ").append(logsStatus.getText()).append("\n");
        sb.append(Lang.s(R.string.tools_rep_games) + "\n");
        Map<String, EdenScanner.ScannedGame> games = EdenScanner.getScannedGames(this);
        for (EdenScanner.ScannedGame g : games.values()) {
            sb.append("- ").append(g.title != null ? g.title : g.tid)
                    .append(" [").append(g.tid).append("]")
                    .append(g.bid != null ? " (BID: " + g.bid + ")" : "")
                    .append(g.version != null ? " (v" + g.version + ")" : "")
                    .append("\n");
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Eden Report", sb.toString()));
            Ui.toast(this, Lang.s(R.string.tools_report_copied));
        }
    }

    private void executeUpdateTask() {
        if (AutoUpdater.isRunning()) {
            Ui.toast(this, Lang.s(R.string.tools_update_running));
            return;
        }
        updateStatus.setText(Lang.s(R.string.tools_update_starting));
        AutoUpdater.runUpdate(this, new AutoUpdater.UpdateListener() {
            @Override
            public void onProgress(final String message, final int percent) {
                runOnUiThread(() -> updateStatus.setText(message + " (" + percent + "%)"));
            }

            @Override
            public void onSuccess(final AutoUpdater.UpdateResult result) {
                runOnUiThread(() -> {
                    updateStatus.setText(AutoUpdater.getLastUpdateInfo(ToolsActivity.this));
                    refreshDbStats();
                    Ui.alert(ToolsActivity.this, Lang.s(R.string.tools_update_done_title), result.summary());
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(() -> {
                    updateStatus.setText(Lang.s(R.string.tools_update_error, error));
                    Ui.toast(ToolsActivity.this, error);
                });
            }
        });
    }

    /** Conta os cheats da base local em segundo plano e mostra o resumo. */
    private void refreshDbStats() {
        if (dbStatus == null) return;
        dbStatus.setText(Lang.s(R.string.ux3_db_counting));
        Util.POOL.execute(() -> {
            try {
                final DbStats.Stats st = DbStats.compute(this);
                final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(Lang.locale());
                runOnUiThread(() -> dbStatus.setText(Lang.s(R.string.ux3_db_summary,
                        nf.format(st.cheats), nf.format(st.games), nf.format(st.builds))));
            } catch (Exception e) {
                runOnUiThread(() -> dbStatus.setText(""));
            }
        });
    }

    private void restoreDatabase() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(Lang.s(R.string.tools_restore_title))
                .setMessage(Lang.s(R.string.tools_restore_msg))
                .setPositiveButton(Lang.s(R.string.tools_restore_btn), (d, w) -> {
                    boolean ok = AutoUpdater.restoreFactoryDatabase(this);
                    if (ok) {
                        updateStatus.setText(AutoUpdater.getLastUpdateInfo(this));
                        refreshDbStats();
                        Ui.toast(this, Lang.s(R.string.tools_restore_ok));
                    } else {
                        Ui.toast(this, Lang.s(R.string.tools_restore_fail));
                    }
                })
                .setNegativeButton(Lang.s(R.string.tools_cancel), null)
                .show();
    }
}
