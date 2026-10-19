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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sources = Sources.all();

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

        // Grid 2x2 de Ações Rápidas (Mais versátil e fácil de tocar no celular)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = Ui.button(this, "🔄 Atualizar", false, v -> load(true));
        Button folder = Ui.button(this, "📁 Pasta Eden", false, v -> folderDialog());
        row1.addView(refresh, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row1.addView(folder, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row1, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button installed = Ui.button(this, "📥 Instalados", false, v ->
                startActivity(new Intent(this, InstalledActivity.class)));
        Button custom = Ui.button(this, "📋 Colar Código", false, v ->
                startActivity(new Intent(this, CustomActivity.class)));
        row2.addView(installed, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        row2.addView(custom, Ui.lp(this, 0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 3));
        root.addView(row2, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        // Card Tecnológico do Banco de Dados Ativo
        LinearLayout srcCard = new LinearLayout(this);
        srcCard.setOrientation(LinearLayout.VERTICAL);
        srcCard.setBackground(Ui.shape(this, Ui.CARD, 10, 1, Ui.BORDER));
        int pCard = Ui.dp(this, 8);
        srcCard.setPadding(pCard, pCard / 2, pCard, pCard / 2);

        TextView srcLabel = Ui.text(this, "BANCO DE DADOS ATIVO", 10, Ui.MUTED, true);
        srcLabel.setTypeface(Typeface.MONOSPACE);
        srcCard.addView(srcLabel);

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
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

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
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

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
                runOnUiThread(() -> {
                    if (gen != generation) return;
                    all = games;
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
            if (!q.isEmpty() && !matches(g, q)) continue;
            shown.add(g);
            int n = g.items.size();
            String id = g.tid.isEmpty() ? "TID DESCONHECIDO" : g.tid;
            adapter.rows.add(new String[]{g.display(), id + " · " + n + (n == 1 ? " pacote" : " pacotes"), "0"});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;
        String s = shown.size() + " de " + all.size() + " jogos disponíveis";
        if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\nBuild ID não encontrado nesta fonte. Experimente outra fonte acima ou use \"Colar código\".";
        }
        status.setText(s);
    }
}
