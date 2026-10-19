package com.example.edencheats;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends BaseActivity {
    private Model.Game game;
    private EditText bidField;
    private Ui.RowAdapter adapter;
    private final List<Model.Item> shown = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        game = Session.game;
        if (game == null) {
            finish();
            return;
        }
        LinearLayout root = Ui.page(this);

        // Header Card do Jogo
        LinearLayout gameCard = new LinearLayout(this);
        gameCard.setOrientation(LinearLayout.VERTICAL);
        gameCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        gameCard.setPadding(p, p, p, p);

        gameCard.addView(Ui.text(this, game.display(), 19, Ui.TEXT, true));

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        int mt = Ui.dp(this, 6);
        metaRow.setPadding(0, mt, 0, 0);

        String tidStr = game.tid.isEmpty() ? "TID DESCONHECIDO" : game.tid;
        TextView tidBadge = Ui.badge(this, tidStr, Ui.ACCENT_BG, Ui.ACCENT, Ui.BORDER_GLOW);
        metaRow.addView(tidBadge);

        TextView countBadge = Ui.text(this, " · " + game.items.size() + " pacote(s)", 12, Ui.MUTED, false);
        metaRow.addView(countBadge);

        gameCard.addView(metaRow);
        root.addView(gameCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        // Filtro por Build ID
        TextView lbl = Ui.text(this, "FILTRAR PELO SEU BUILD ID (OPCIONAL)", 11, Ui.MUTED, true);
        lbl.setTypeface(Typeface.MONOSPACE);
        root.addView(lbl, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        bidField = Ui.monoEdit(this, "Ex.: E7E1382D5EB931B0 (16 hex)");
        String q = Session.query == null ? "" : Session.query.toUpperCase(Locale.ROOT);
        if (q.length() >= 4 && q.matches("[0-9A-F]+")) bidField.setText(q);
        bidField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {}

            @Override
            public void afterTextChanged(Editable e) {
                refresh();
            }
        });
        root.addView(bidField, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4));

        TextView tip = Ui.text(this, "💡 Itens compatíveis com o seu Build ID ficam destacados em verde neon no topo.",
                11, Ui.GOOD, false);
        root.addView(tip, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 2));

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((pView, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            Session.item = shown.get(pos);
            String bidVal = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
            Session.targetBuildId = bidVal;
            startActivity(new Intent(this, ItemActivity.class));
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));
        setContentView(root);
        refresh();
    }

    private void refresh() {
        String q = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
        List<Model.Item> match = new ArrayList<>();
        List<Model.Item> other = new ArrayList<>();
        for (Model.Item it : game.items) {
            if (!q.isEmpty() && it.cheat && it.bid.contains(q)) match.add(it);
            else other.add(it);
        }
        shown.clear();
        shown.addAll(match);
        shown.addAll(other);
        adapter.rows.clear();
        for (Model.Item it : shown) {
            boolean m = match.contains(it);
            String sub = (it.cheat ? "Cheats" : "Mod") + " · " + it.files.size() + " arquivo(s)";
            long size = it.totalSize();
            if (size > 0) sub += " · " + Util.mb(size);
            if (m) sub += " · 100% Compatível";
            adapter.rows.add(new String[]{(m ? "⚡ " : "") + it.title, sub, m ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
    }
}
