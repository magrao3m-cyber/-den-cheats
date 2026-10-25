package com.example.edencheats;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {
    private Sources.Source[] sources;
    private Spinner spinner;
    private android.widget.EditText search;
    private TextView status;
    private Ui.RowAdapter adapter;
    private List<Model.Game> all = new ArrayList<>();
    private final List<Model.Game> shown = new ArrayList<>();
    private int generation = 0;
    private int current = -1;

    private boolean filterMyGames = false;
    private TextView chipMyGames;
    private TextView chipAllGames;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Names.ensure(this);
        sources = Sources.all();

        // Se já houver jogos detectados no Eden, abre direto na aba "Meus Jogos"
        int scannedCount = EdenScanner.getCount(this);
        if (scannedCount > 0) filterMyGames = true;

        LinearLayout root = Ui.page(this);

        // Header Tecnológico (Área de Identidade)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        // Logo Principal: Raio com brilho neon difuso + SWITCHFORGE em caixa alta geométrica
        LinearLayout logoRow = new LinearLayout(this);
        logoRow.setOrientation(LinearLayout.HORIZONTAL);
        logoRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView bolt = new TextView(this);
        bolt.setText("⚡");
        bolt.setTextSize(21);
        bolt.setTextColor(Ui.ACCENT);
        bolt.setShadowLayer(Ui.dp(this, 8), 0, 0, Ui.ACCENT);
        logoRow.addView(bolt);

        TextView title = new TextView(this);
        title.setText(" SWITCHFORGE");
        title.setTextSize(21);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        logoRow.addView(title);
        titleCol.addView(logoRow);

        // Linha de status compacta abaixo do logo
        TextView sysStatus = new TextView(this);
        sysStatus.setText("[ TERMINAL V1.7.0 ] // ESTÁVEL");
        sysStatus.setTextSize(10);
        sysStatus.setTextColor(Ui.ICON_MUTED);
        sysStatus.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        int mt = Ui.dp(this, 2);
        sysStatus.setPadding(0, mt, 0, 0);
        titleCol.addView(sysStatus);

        header.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Badge "Cyber Ed.": Pílula minimalista com fundo preto e borda fina em ciano
        TextView verBadge = new TextView(this);
        verBadge.setText("CYBER ED.");
        verBadge.setTextSize(10);
        verBadge.setTextColor(Ui.ACCENT);
        verBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        verBadge.setBackground(Ui.shape(this, 0xFF000000, 12, 1, Ui.ACCENT));
        int pxB = Ui.dp(this, 10);
        int pyB = Ui.dp(this, 4);
        verBadge.setPadding(pxB, pyB, pxB, pyB);
        header.addView(verBadge);

        root.addView(header, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // 1. Grid 3x2 de Cards Tecnológicos Unificados (Ações Rápidas)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        View scanBtn = Ui.techCardButton(this, "Varredura Eden", "[SCAN]", true, v -> doScan());
        View folderBtn = Ui.techCardButton(this, "Pasta Eden", "[PATH]", false, v -> folderDialog());
        row1.addView(scanBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row1.addView(folderBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row1, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        View installedBtn = Ui.techCardButton(this, "Cheats Ativos", "[MODS]", false, v ->
                startActivity(new Intent(this, InstalledActivity.class)));
        View savesBtn = Ui.techCardButton(this, "Gerenciar Saves", "[SAVE]", false, v ->
                startActivity(new Intent(this, SaveManagerActivity.class)));
        row2.addView(installedBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row2.addView(savesBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row2, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        View customBtn = Ui.techCardButton(this, "Colar Código", "[RAW]", false, v ->
                startActivity(new Intent(this, CustomActivity.class)));
        View toolsBtn = Ui.techCardButton(this, "Ferramentas & Logs", "[TOOL]", false, v ->
                startActivity(new Intent(this, ToolsActivity.class)));
        row3.addView(customBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row3.addView(toolsBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row3, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        // 2. Console Unificado de Controle (Filtros, Servidor, Trio de Ações e Busca)
        LinearLayout console = new LinearLayout(this);
        console.setOrientation(LinearLayout.VERTICAL);
        console.setBackground(Ui.shape(this, Ui.CARD, 10, 1, Ui.BORDER));
        int pConsole = Ui.dp(this, 10);
        console.setPadding(pConsole, pConsole, pConsole, pConsole);

        // Linha Superior: Cabeçalho do Console + Trio Unificado de Ações
        LinearLayout consoleTop = new LinearLayout(this);
        consoleTop.setOrientation(LinearLayout.HORIZONTAL);
        consoleTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView consoleLabel = Ui.text(this, "CONSOLE DE OPERAÇÕES", 10, Ui.MUTED, true);
        consoleLabel.setTypeface(Typeface.MONOSPACE);
        consoleTop.addView(consoleLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View fpsPill = Ui.techActionPill(this, "⚡", Ui.GOOD, "60FPS", v -> {
            if (spinner != null) {
                for (int i = 0; i < sources.length; i++) {
                    if (sources[i].label.contains("60FPS")) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        });
        consoleTop.addView(fpsPill);

        View refreshPill = Ui.techActionPill(this, "🔄", Ui.ACCENT, "RECARREGAR", v -> load(true));
        LinearLayout.LayoutParams lpRef = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRef.setMargins(Ui.dp(this, 5), 0, 0, 0);
        consoleTop.addView(refreshPill, lpRef);

        View updatePill = Ui.techActionPill(this, "🚀", Ui.WARN, "ATUALIZAÇÃO", v -> showUpdateDialog());
        LinearLayout.LayoutParams lpUpd = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpUpd.setMargins(Ui.dp(this, 5), 0, 0, 0);
        consoleTop.addView(updatePill, lpUpd);
        console.addView(consoleTop);

        // Seletor de Servidor em container tech escuro
        LinearLayout spinnerBox = new LinearLayout(this);
        spinnerBox.setOrientation(LinearLayout.HORIZONTAL);
        spinnerBox.setGravity(Gravity.CENTER_VERTICAL);
        spinnerBox.setBackground(Ui.shape(this, 0xFF0E121A, 6, 1, Ui.BORDER));
        spinnerBox.setPadding(Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2));

        spinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (Sources.Source s : sources) labels.add(s.label);
        ArrayAdapter<String> sa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Ui.ACCENT);
                    ((TextView) v).setTextSize(12);
                    ((TextView) v).setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                }
                return v;
            }
        };
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa);
        spinnerBox.addView(spinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        console.addView(spinnerBox, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Abas de Filtro: [ ◈ Meus Jogos (X) ] vs [ ◈ Todos os Jogos ]
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        chipMyGames = Ui.chip(this, "◈ Meus Jogos (" + scannedCount + ")", filterMyGames, v -> {
            filterMyGames = true;
            updateFilterChips();
            applyFilter();
        });

        chipAllGames = Ui.chip(this, "◈ Todos os Jogos", !filterMyGames, v -> {
            filterMyGames = false;
            updateFilterChips();
            applyFilter();
        });

        LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChip.setMargins(0, 0, Ui.dp(this, 8), 0);
        filterRow.addView(chipMyGames, lpChip);
        filterRow.addView(chipAllGames);
        console.addView(filterRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Caixa de Busca Integrada ao Console
        search = Ui.monoEdit(this, "⌕ Buscar jogo, Title ID ou Build ID...");
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
        console.addView(search, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        root.addView(console, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            Session.game = shown.get(pos);
            startActivity(new Intent(this, DetailActivity.class));
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));

        setContentView(root);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current) {
                    current = position;
                    load(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onFolderChanged() {
        super.onFolderChanged();
        doScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFilterChips();
        applyFilter();
    }

    private void updateFilterChips() {
        int count = EdenScanner.getCount(this);
        chipMyGames.setText("◈ Meus Jogos (" + count + ")");
        if (filterMyGames) {
            chipMyGames.setTextColor(0xFF0A0D14);
            chipMyGames.setBackground(Ui.shape(this, Ui.ACCENT, 14, 0, 0));
            chipAllGames.setTextColor(Ui.ICON_MUTED);
            chipAllGames.setBackground(Ui.shape(this, 0x33121620, 14, 1, Ui.BORDER));
        } else {
            chipAllGames.setTextColor(0xFF0A0D14);
            chipAllGames.setBackground(Ui.shape(this, Ui.ACCENT, 14, 0, 0));
            chipMyGames.setTextColor(Ui.ICON_MUTED);
            chipMyGames.setBackground(Ui.shape(this, 0x33121620, 14, 1, Ui.BORDER));
        }
    }

    private void doScan() {
        if (SafStore.getTree(this) == null) {
            folderDialog();
            return;
        }
        status.setText("Iniciando varredura profunda no Eden…");
        Util.POOL.execute(() -> {
            try {
                final int count = EdenScanner.scanEden(this, msg -> runOnUiThread(() -> status.setText(msg)));
                runOnUiThread(() -> {
                    Ui.toast(this, "Varredura concluída! " + count + " jogo(s) detectados no seu Eden.");
                    if (count > 0) filterMyGames = true;
                    updateFilterChips();
                    applyFilter();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    status.setText("Erro na varredura: " + e.getMessage()
                            + "\nVerifique se a pasta selecionada é a do Eden.");
                    Ui.toast(this, "Erro: " + e.getMessage());
                });
            }
        });
    }

    private void load(final boolean refresh) {
        if (current < 0) return;
        final Sources.Source src = sources[current];
        final int gen = ++generation;
        all = new ArrayList<>();
        applyFilter();
        status.setText("Sincronizando banco de dados…");
        Util.POOL.execute(() -> {
            try {
                final List<Model.Game> games = src.load(MainActivity.this, refresh, msg -> runOnUiThread(() -> {
                    if (gen == generation) status.setText(msg);
                }));

                // Se houver pasta configurada mas ainda nenhum jogo escaneado, realiza a primeira varredura silenciosa
                if (SafStore.getTree(MainActivity.this) != null && EdenScanner.getCount(MainActivity.this) == 0) {
                    try {
                        EdenScanner.scanEden(MainActivity.this, null);
                    } catch (Exception ignored) {}
                }

                runOnUiThread(() -> {
                    if (gen != generation) return;
                    all = games;
                    updateFilterChips();
                    applyFilter();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    if (gen != generation) return;
                    status.setText("Erro de conexão: " + e.getMessage()
                            + "\nVerifique a rede e toque em Atualizar.");
                });
            }
        });
    }

    private static boolean matches(Model.Game g, String q) {
        if (g.display().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (g.tid.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (Model.Item it : g.items) {
            if (!it.bid.isEmpty() && it.bid.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        Session.query = q;
        shown.clear();
        adapter.rows.clear();

        for (Model.Game g : all) {
            boolean isInst = EdenScanner.isInstalled(this, g.tid);
            if (filterMyGames && !isInst) continue;
            if (!q.isEmpty() && !matches(g, q)) continue;

            shown.add(g);
            int n = g.items.size();
            String id = g.tid.isEmpty() ? "TID DESCONHECIDO" : g.tid;
            String detectedBid = EdenScanner.getBuildId(this, g.tid);

            if (g.name == null || g.name.isEmpty() || g.name.equalsIgnoreCase(g.tid)) {
                String known = Names.get(g.tid);
                if (known != null && !known.isEmpty()) {
                    g.name = known;
                } else {
                    String scannedTitle = EdenScanner.getTitle(this, g.tid);
                    if (scannedTitle != null && !scannedTitle.isEmpty()) {
                        g.name = scannedTitle;
                    }
                }
            }

            String sub = id + " · " + n + (n == 1 ? " pacote" : " pacotes");
            if (detectedBid != null) {
                sub = "⚡ BID: " + detectedBid + " · " + sub;
            }
            String title = g.display();
            adapter.rows.add(new String[]{title, sub, isInst ? "1" : "0", g.tid});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;

        int totalInstalled = EdenScanner.getCount(this);
        String s = shown.size() + " de " + all.size() + " jogos"
                + (filterMyGames ? " (Filtrando Meus Jogos no Eden: " + totalInstalled + ")" : "");
        if (shown.isEmpty() && filterMyGames) {
            s = "Nenhum dos " + totalInstalled + " jogos detectados no Eden foi encontrado nesta fonte.\n"
                    + "Toque em '◈ Todos os Jogos' para buscar no catálogo completo ou troque o Banco de Dados.";
        } else if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\nBuild ID não encontrado nesta fonte. Experimente outra fonte acima ou use \"Colar código\".";
        }
        status.setText(s);
    }

    private void showUpdateDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p);

        root.addView(Ui.text(this, "⚡ TAREFA DE ATUALIZAÇÃO", 18, Ui.TEXT, true));
        root.addView(Ui.text(this, "Busca novos cheats nos servidores remotos, desduplica códigos hexadecimais e incorpora diretamente ao banco local.", 12, Ui.MUTED, false));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.shape(this, Ui.CARD, 8, 1, Ui.BORDER));
        int pb = Ui.dp(this, 10);
        box.setPadding(pb, pb, pb, pb);
        TextView infoTxt = Ui.text(this, AutoUpdater.getLastUpdateInfo(this), 12, Ui.TEXT, false);
        box.addView(infoTxt);
        root.addView(box, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        TextView progTxt = Ui.text(this, "", 12, Ui.ACCENT, false);
        root.addView(progTxt, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        Button runBtn = Ui.button(this, "🚀 INICIAR ATUALIZAÇÃO", true, null);
        root.addView(runBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        b.setView(root);
        final android.app.AlertDialog dlg = b.create();

        runBtn.setOnClickListener(v -> {
            if (AutoUpdater.isRunning()) {
                Ui.toast(this, "Atualização já em andamento.");
                return;
            }
            runBtn.setEnabled(false);
            runBtn.setText("⏳ Atualizando Banco...");
            AutoUpdater.runUpdate(this, new AutoUpdater.UpdateListener() {
                @Override
                public void onProgress(final String message, final int percent) {
                    runOnUiThread(() -> progTxt.setText(message + " (" + percent + "%)"));
                }

                @Override
                public void onSuccess(final AutoUpdater.UpdateResult result) {
                    runOnUiThread(() -> {
                        dlg.dismiss();
                        Ui.alert(MainActivity.this, "⚡ Atualização Concluída", result.summary());
                        load(true);
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(() -> {
                        runBtn.setEnabled(true);
                        runBtn.setText("🚀 TENTAR NOVAMENTE");
                        progTxt.setText("Erro: " + error);
                        Ui.toast(MainActivity.this, error);
                    });
                }
            });
        });

        dlg.show();
    }
}
