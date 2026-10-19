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

        // Header Tecnológico
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.addView(Ui.text(this, "⚡ EDEN CHEATS", 22, Ui.TEXT, true));
        titleCol.addView(Ui.text(this, "Gerenciador Tecnológico de Cheats & Mods", 12, Ui.MUTED, false));
        header.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView verBadge = Ui.badge(this, "CYBER ED.", Ui.ACCENT_BG, Ui.ACCENT, Ui.BORDER_GLOW);
        header.addView(verBadge);
        root.addView(header, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Grid 3x2 de Ações Rápidas (Varredura, Pastas, Cheats Instalados, Saves, Colar e Diagnóstico)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button scanBtn = Ui.button(this, "🔍 Varredura Eden", true, v -> doScan());
        Button folderBtn = Ui.button(this, "📁 Pasta Eden", false, v -> folderDialog());
        row1.addView(scanBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row1.addView(folderBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row1, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button installedBtn = Ui.button(this, "📥 Cheats Instalados", false, v ->
                startActivity(new Intent(this, InstalledActivity.class)));
        Button savesBtn = Ui.button(this, "💾 Gerenciar Saves", false, v ->
                startActivity(new Intent(this, SaveManagerActivity.class)));
        row2.addView(installedBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row2.addView(savesBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row2, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        Button customBtn = Ui.button(this, "📋 Colar Código", false, v ->
                startActivity(new Intent(this, CustomActivity.class)));
        Button toolsBtn = Ui.button(this, "🛠️ Ferramentas & Logs", false, v ->
                startActivity(new Intent(this, ToolsActivity.class)));
        row3.addView(customBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row3.addView(toolsBtn, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row3, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        // Card Tecnológico do Banco de Dados Ativo
        LinearLayout srcCard = new LinearLayout(this);
        srcCard.setOrientation(LinearLayout.VERTICAL);
        srcCard.setBackground(Ui.shape(this, Ui.CARD, 10, 1, Ui.BORDER));
        int pCard = Ui.dp(this, 8);
        srcCard.setPadding(pCard, pCard / 2, pCard, pCard / 2);

        LinearLayout srcHeader = new LinearLayout(this);
        srcHeader.setOrientation(LinearLayout.HORIZONTAL);
        srcHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView srcLabel = Ui.text(this, "BANCO DE DADOS ATIVO", 10, Ui.MUTED, true);
        srcLabel.setTypeface(Typeface.MONOSPACE);
        srcHeader.addView(srcLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView fpsBadge = Ui.badge(this, "⚡ 60FPS / GFX", Ui.GOOD_BG, Ui.GOOD, Ui.GOOD);
        fpsBadge.setOnClickListener(v -> {
            if (spinner != null) {
                for (int i = 0; i < sources.length; i++) {
                    if (sources[i].label.contains("60FPS")) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        });
        srcHeader.addView(fpsBadge);

        TextView refreshIco = Ui.badge(this, "🔄 RECARREGAR", Ui.ACCENT_BG, Ui.ACCENT, Ui.BORDER);
        LinearLayout.LayoutParams lpRef = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRef.setMargins(Ui.dp(this, 6), 0, 0, 0);
        refreshIco.setOnClickListener(v -> load(true));
        srcHeader.addView(refreshIco, lpRef);
        srcCard.addView(srcHeader);

        spinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (Sources.Source s : sources) labels.add(s.label);
        ArrayAdapter<String> sa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Ui.ACCENT);
                    ((TextView) v).setTextSize(13);
                    ((TextView) v).setTypeface(Typeface.DEFAULT_BOLD);
                }
                return v;
            }
        };
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa);
        srcCard.addView(spinner);
        root.addView(srcCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 3));

        // Barra de Filtro de Abas: [ 🎮 Meus Jogos (X) ] vs [ 🌐 Todos os Jogos ]
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        chipMyGames = Ui.chip(this, "🎮 Meus Jogos (" + scannedCount + ")", filterMyGames, v -> {
            filterMyGames = true;
            updateFilterChips();
            applyFilter();
        });

        chipAllGames = Ui.chip(this, "🌐 Todos os Jogos", !filterMyGames, v -> {
            filterMyGames = false;
            updateFilterChips();
            applyFilter();
        });

        LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChip.setMargins(0, 0, Ui.dp(this, 8), 0);
        filterRow.addView(chipMyGames, lpChip);
        filterRow.addView(chipAllGames);
        root.addView(filterRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 3));

        // Caixa de Busca
        search = Ui.edit(this, "🔍 Buscar jogo, Title ID ou Build ID...");
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
        chipMyGames.setText("🎮 Meus Jogos (" + count + ")");
        if (filterMyGames) {
            chipMyGames.setTextColor(0xFF0A0D14);
            chipMyGames.setBackground(Ui.shape(this, Ui.ACCENT, 16, 0, 0));
            chipAllGames.setTextColor(Ui.MUTED);
            chipAllGames.setBackground(Ui.shape(this, Ui.CARD, 16, 1, Ui.BORDER));
        } else {
            chipAllGames.setTextColor(0xFF0A0D14);
            chipAllGames.setBackground(Ui.shape(this, Ui.ACCENT, 16, 0, 0));
            chipMyGames.setTextColor(Ui.MUTED);
            chipMyGames.setBackground(Ui.shape(this, Ui.CARD, 16, 1, Ui.BORDER));
        }
    }

    private void doScan() {
        if (SafStore.getTree(this) == null) {
            folderDialog();
            return;
        }
        status.setText("🔍 Iniciando varredura profunda no Eden…");
        Util.POOL.execute(() -> {
            try {
                final int count = EdenScanner.scanEden(this, msg -> runOnUiThread(() -> status.setText(msg)));
                runOnUiThread(() -> {
                    Ui.toast(this, "⚡ Varredura concluída! " + count + " jogo(s) detectados no seu Eden.");
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
            String title = (isInst ? "⚡ " : "") + g.display();
            adapter.rows.add(new String[]{title, sub, isInst ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;

        int totalInstalled = EdenScanner.getCount(this);
        String s = shown.size() + " de " + all.size() + " jogos"
                + (filterMyGames ? " (Filtrando Meus Jogos no Eden: " + totalInstalled + ")" : "");
        if (shown.isEmpty() && filterMyGames) {
            s = "Nenhum dos " + totalInstalled + " jogos detectados no Eden foi encontrado nesta fonte.\n"
                    + "Toque em '🌐 Todos os Jogos' para buscar no catálogo completo ou troque o Banco de Dados.";
        } else if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\nBuild ID não encontrado nesta fonte. Experimente outra fonte acima ou use \"Colar código\".";
        }
        status.setText(s);
    }
}
