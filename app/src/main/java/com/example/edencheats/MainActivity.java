package com.example.edencheats;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
        root.addView(Ui.text(this, "Eden Cheats Manager", 22, Ui.TEXT, true));
        root.addView(Ui.text(this, "Cheats e mods de Switch para o Eden no Android", 13, Ui.MUTED, false));

        spinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (Sources.Source s : sources) labels.add(s.label);
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa);
        root.addView(spinner, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        search = Ui.edit(this, "Buscar por nome, Title ID ou Build ID");
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

        Button refresh = Ui.button(this, "Atualizar", v -> load(true));
        Button installed = Ui.button(this, "Instalados", v ->
                startActivity(new Intent(this, InstalledActivity.class)));
        Button custom = Ui.button(this, "Colar código", v ->
                startActivity(new Intent(this, CustomActivity.class)));
        Button folder = Ui.button(this, "Pasta", v -> folderDialog());
        root.addView(Ui.buttonRow(this, refresh, installed, custom, folder));

        status = Ui.text(this, "", 13, Ui.MUTED, false);
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
        status.setText("Carregando…");
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
                    status.setText("Erro: " + e.getMessage()
                            + "\nVerifique a internet e toque em Atualizar.");
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
            String id = g.tid.isEmpty() ? "TID desconhecido" : g.tid;
            adapter.rows.add(new String[]{g.display(), id + " · " + n + (n == 1 ? " item" : " itens"), "0"});
        }
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) return;
        String s = shown.size() + " de " + all.size() + " jogos";
        if (shown.isEmpty() && Util.isHex16(q.toUpperCase(Locale.ROOT))) {
            s += "\nBuild ID não encontrado nesta fonte. Tente outra fonte ou use \"Colar código\".";
        }
        status.setText(s);
    }
}
