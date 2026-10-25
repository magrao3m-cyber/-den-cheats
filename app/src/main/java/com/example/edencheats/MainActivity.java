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
        sysStatus.setText(Lang.s(R.string.main_terminal_status));
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

        // Seletor de idioma: pílula "🌐 PT" que abre a escolha entre Português, English e Español
        TextView langBadge = new TextView(this);
        langBadge.setText("\uD83C\uDF10 " + Lang.shortCode());
        langBadge.setTextSize(10);
        langBadge.setTextColor(Ui.TEXT);
        langBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        langBadge.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        langBadge.setPadding(pxB, pyB, pxB, pyB);
        langBadge.setClickable(true);
        langBadge.setOnClickListener(v -> Lang.showPicker(this));
        LinearLayout.LayoutParams lpLang = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLang.setMargins(Ui.dp(this, 6), 0, 0, 0);
        header.addView(langBadge, lpLang);

        root.addView(header, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // 1. Ações rápidas em grade adaptável (3 colunas em telas largas, 2 no celular).
        View[] quickActions = new View[] {
                Ui.techCardButton(this, Lang.s(R.string.main_btn_scan), "[SCAN]", true, v -> doScan()),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_folder), "[PATH]", false, v -> folderDialog()),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_active_cheats), "[MODS]", false, v ->
                        startActivity(new Intent(this, InstalledActivity.class))),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_saves), "[SAVE]", false, v ->
                        startActivity(new Intent(this, SaveManagerActivity.class))),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_paste_code), "[RAW]", false, v ->
                        startActivity(new Intent(this, CustomActivity.class))),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_tools), "[TOOL]", false, v ->
                        startActivity(new Intent(this, ToolsActivity.class))),
                Ui.techCardButton(this, Lang.s(R.string.main_btn_coffee), "[APOIO]", false, v ->
                        Util.openDonation(this))
        };
        int actionColumns = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 3 : 2;
        for (int start = 0; start < quickActions.length; start += actionColumns) {
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < actionColumns && start + col < quickActions.length; col++) {
                actionRow.addView(quickActions[start + col],
                        Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
            }
            root.addView(actionRow, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));
        }

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

        TextView consoleLabel = Ui.text(this, Lang.s(R.string.main_console_label), 10, Ui.MUTED, true);
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

        View refreshPill = Ui.techActionPill(this, "🔄", Ui.ACCENT, Lang.s(R.string.main_pill_reload), v -> load(true));
        LinearLayout.LayoutParams lpRef = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRef.setMargins(Ui.dp(this, 5), 0, 0, 0);
        consoleTop.addView(refreshPill, lpRef);

        View updatePill = Ui.techActionPill(this, "🚀", Ui.WARN, Lang.s(R.string.main_pill_update), v -> showUpdateDialog());
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

        chipMyGames = Ui.chip(this, Lang.s(R.string.main_chip_my_games, scannedCount), filterMyGames, v -> {
            filterMyGames = true;
            updateFilterChips();
            applyFilter();
        });

        chipAllGames = Ui.chip(this, Lang.s(R.string.main_chip_all_games), !filterMyGames, v -> {
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
        search = Ui.monoEdit(this, Lang.s(R.string.main_search_hint));
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
        chipMyGames.setText(Lang.s(R.string.main_chip_my_games, count));
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
        status.setText(Lang.s(R.string.main_scan_starting));
        Util.POOL.execute(() -> {
            try {
                final int count = EdenScanner.scanEden(this, msg -> runOnUiThread(() -> status.setText(msg)));
                runOnUiThread(() -> {
                    Ui.toast(this, Lang.s(R.string.main_scan_done, count));
                    if (count > 0) filterMyGames = true;
                    updateFilterChips();
                    applyFilter();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    status.setText(Lang.s(R.string.main_scan_error, e.getMessage()));
                    Ui.toast(this, Lang.s(R.string.main_error_prefix, e.getMessage()));
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
        status.setText(Lang.s(R.string.main_syncing_db));
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
                    status.setText(Lang.s(R.string.main_connection_error, e.getMessage()));
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
            String id = g.tid.isEmpty() ? Lang.s(R.string.main_unknown_tid) : g.tid;
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

            String sub = Lang.s(n == 1 ? R.string.main_sub_package_one : R.string.main_sub_package_many, id, n);
            if (detectedBid != null) {
                sub = "⚡ BID: " + detectedBid + " · " + sub;
            }
            String title = g.display();
            adapter.rows.add(new String[]{title, sub, isInst ? "1" : "0", g.tid});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;

        int totalInstalled = EdenScanner.getCount(this);
        String s = Lang.s(R.string.main_status_count, shown.size(), all.size())
                + (filterMyGames ? " " + Lang.s(R.string.main_status_filtering, totalInstalled) : "");
        if (shown.isEmpty() && filterMyGames) {
            s = Lang.s(R.string.main_none_found, totalInstalled);
        } else if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\n" + Lang.s(R.string.main_build_id_not_found);
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

        root.addView(Ui.text(this, Lang.s(R.string.main_update_task_title), 18, Ui.TEXT, true));
        root.addView(Ui.text(this, Lang.s(R.string.main_update_task_desc), 12, Ui.MUTED, false));

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

        Button runBtn = Ui.button(this, Lang.s(R.string.main_update_start), true, null);
        root.addView(runBtn, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        b.setView(root);
        final android.app.AlertDialog dlg = b.create();

        runBtn.setOnClickListener(v -> {
            if (AutoUpdater.isRunning()) {
                Ui.toast(this, Lang.s(R.string.main_update_running));
                return;
            }
            runBtn.setEnabled(false);
            runBtn.setText(Lang.s(R.string.main_update_updating));
            AutoUpdater.runUpdate(this, new AutoUpdater.UpdateListener() {
                @Override
                public void onProgress(final String message, final int percent) {
                    runOnUiThread(() -> progTxt.setText(message + " (" + percent + "%)"));
                }

                @Override
                public void onSuccess(final AutoUpdater.UpdateResult result) {
                    runOnUiThread(() -> {
                        dlg.dismiss();
                        Ui.alert(MainActivity.this, Lang.s(R.string.main_update_done_title), result.summary());
                        load(true);
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(() -> {
                        runBtn.setEnabled(true);
                        runBtn.setText(Lang.s(R.string.main_update_retry));
                        progTxt.setText(Lang.s(R.string.main_error_prefix, error));
                        Ui.toast(MainActivity.this, error);
                    });
                }
            });
        });

        dlg.show();
    }
}
