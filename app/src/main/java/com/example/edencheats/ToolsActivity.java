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

        // Header Card
        LinearLayout headCard = new LinearLayout(this);
        headCard.setOrientation(LinearLayout.VERTICAL);
        headCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        headCard.setPadding(p, p, p, p);
        headCard.addView(Ui.text(this, "🛠️ DIAGNÓSTICO & FERRAMENTAS", 19, Ui.TEXT, true));
        headCard.addView(Ui.text(this, "Verifique chaves, logs e a integridade da pasta do emulador Eden", 12, Ui.MUTED, false));
        root.addView(headCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // 1. Card Pasta do Eden
        LinearLayout folderCard = cardSection(this, "DIRETÓRIO DO EDEN");
        folderStatus = Ui.text(this, "Carregando…", 13, Ui.TEXT, false);
        folderCard.addView(folderStatus);
        Button changeFolderBtn = Ui.button(this, "📁 Alterar Pasta do Eden", false, v -> folderDialog());
        folderCard.addView(changeFolderBtn, Ui.lp(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(folderCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 2. Card Chaves (prod.keys / title.keys)
        LinearLayout keysCard = cardSection(this, "CHAVES DE CRIPTOGRAFIA (PROD.KEYS)");
        keysStatus = Ui.text(this, "Verificando chaves…", 13, Ui.TEXT, false);
        keysCard.addView(keysStatus);
        root.addView(keysCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 3. Card Logs de Execução & Limpeza
        LinearLayout logCard = cardSection(this, "REGISTROS & LOGS DO EMULADOR");
        logsStatus = Ui.text(this, "Calculando tamanho de logs…", 13, Ui.TEXT, false);
        logCard.addView(logsStatus);
        Button cleanLogsBtn = Ui.button(this, "🧹 Limpar / Esvaziar Arquivos de Log", true, v -> clearLogs());
        logCard.addView(cleanLogsBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        root.addView(logCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // 4. Card Varredura & Cache do App
        LinearLayout scannerCard = cardSection(this, "VARREDURA & CACHE LOCAL");
        scannerStatus = Ui.text(this, "Carregando informações…", 13, Ui.TEXT, false);
        scannerCard.addView(scannerStatus);
        LinearLayout cacheRow = new LinearLayout(this);
        cacheRow.setOrientation(LinearLayout.HORIZONTAL);
        Button clearCacheBtn = Ui.button(this, "🗑️ Limpar Cache", false, v -> clearAppCache());
        Button copyReportBtn = Ui.button(this, "📋 Copiar Relatório", false, v -> copyReport());
        cacheRow.addView(clearCacheBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        cacheRow.addView(copyReportBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 2));
        scannerCard.addView(cacheRow);
        root.addView(scannerCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
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
        card.setBackground(Ui.shape(c, Ui.CARD, 10, 1, Ui.BORDER));
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
        scannerStatus.setText("⚡ " + gameCount + " jogo(s) detectados pelo motor de varredura.\n"
                + "Build IDs mapeados e prontos para auto-preenchimento.");

        Util.POOL.execute(() -> {
            String kStatus = "⚠️ Nenhuma pasta do Eden configurada.";
            String lStatus = "Sem acesso a logs.";
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
                            kStatus = "✅ prod.keys detectado e pronto para descriptografia.\n";
                            if (title != null) kStatus += "✅ title.keys detectado.";
                            else kStatus += "ℹ️ title.keys não encontrado (opcional para a maioria dos jogos).";
                        } else {
                            kStatus = "⚠️ Arquivo prod.keys NÃO encontrado na pasta keys/.\n"
                                    + "O emulador precisa dele para carregar jogos e identificar Title IDs.";
                        }
                    } else {
                        kStatus = "ℹ️ Pasta keys/ não encontrada neste diretório.";
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
                        lStatus = "Tamanho dos logs: " + Util.mb(logBytes)
                                + (logBytes > 10 * 1048576L ? " ⚠️ (Grande: limpar logs melhora o desempenho)" : " ✅ (Tamanho normal)");
                    } else {
                        lStatus = "Nenhum arquivo de log com tamanho significativo encontrado.";
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
            Ui.toast(this, "Selecione a pasta do Eden primeiro.");
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
                if (c > 0) Ui.toast(this, "✔ Arquivos de log esvaziados com sucesso! Espaço liberado.");
                else Ui.toast(this, "Nenhum log precisou ser limpo.");
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
                Ui.toast(this, "Cache limpo: " + Util.mb(f) + " liberados.");
                refresh();
            });
        });
    }

    private void copyReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SWITCHFORGE - DIAGNÓSTICO ===\n");
        sb.append("Pasta SAF: ").append(SafStore.describe(this)).append("\n");
        sb.append("Chaves: ").append(keysStatus.getText()).append("\n");
        sb.append("Logs: ").append(logsStatus.getText()).append("\n");
        sb.append("Jogos no Eden:\n");
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
            Ui.toast(this, "📋 Relatório de diagnóstico copiado!");
        }
    }
}
