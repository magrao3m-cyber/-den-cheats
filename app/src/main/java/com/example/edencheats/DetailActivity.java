package com.example.edencheats;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

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
        root.addView(Ui.text(this, game.display(), 20, Ui.TEXT, true));
        root.addView(Ui.text(this, "Title ID: " + (game.tid.isEmpty() ? "(desconhecido)" : game.tid),
                13, Ui.MUTED, false));

        bidField = Ui.edit(this, "Seu Build ID (opcional, 16 caracteres)");
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
        root.addView(Ui.text(this, "Itens com o mesmo Build ID do seu jogo aparecem em verde, no topo.",
                12, Ui.MUTED, false));

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            Session.item = shown.get(pos);
            String b = bidField.getText().toString().trim().toUpperCase(Locale.ROOT);
            Session.targetBuildId = b;
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
            String sub = (it.cheat ? "Arquivo de cheats" : "Mod") + " · " + it.files.size() + " arquivo(s)";
            long size = it.totalSize();
            if (size > 0) sub += " · " + Util.mb(size);
            if (m) sub += " · combina com seu Build ID";
            adapter.rows.add(new String[]{(m ? "✔ " : "") + it.title, sub, m ? "1" : "0"});
        }
        adapter.notifyDataSetChanged();
    }
}
