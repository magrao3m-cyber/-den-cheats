package com.example.edencheats;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Lista (e apaga) os mods/cheats já instalados em load/<TitleID>/<Mod>/. */
public class InstalledActivity extends BaseActivity {
    private static final class Mod {
        String tid;
        String name;
        String docId;
    }

    private TextView status;
    private Ui.RowAdapter adapter;
    private final List<Mod> mods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        root.addView(Ui.text(this, "Instalados", 20, Ui.TEXT, true));
        root.addView(Ui.text(this, "Toque em um item para apagar.", 13, Ui.MUTED, false));
        status = Ui.text(this, "", 13, Ui.MUTED, false);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < mods.size()) confirmDelete(mods.get(pos));
        });
        root.addView(list, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onFolderChanged() {
        refresh();
    }

    private void refresh() {
        if (!ensureFolder()) {
            status.setText("Escolha a pasta de destino primeiro.");
            return;
        }
        status.setText("Lendo…");
        Util.POOL.execute(() -> {
            final List<Mod> found = new ArrayList<>();
            String err = null;
            try {
                Uri tree = SafStore.getTree(this);
                Uri load = SafStore.loadDir(this, false);
                if (tree != null && load != null) {
                    for (SafStore.Entry t : SafStore.list(getContentResolver(), tree, load)) {
                        if (!t.dir) continue;
                        Uri tUri = SafStore.doc(tree, t.docId);
                        for (SafStore.Entry m : SafStore.list(getContentResolver(), tree, tUri)) {
                            if (!m.dir) continue;
                            Mod mod = new Mod();
                            mod.tid = t.name;
                            mod.name = m.name;
                            mod.docId = m.docId;
                            found.add(mod);
                        }
                    }
                }
            } catch (Exception e) {
                err = e.getMessage();
            }
            final String error = err;
            Names.ensure(this);
            runOnUiThread(() -> {
                mods.clear();
                mods.addAll(found);
                adapter.rows.clear();
                for (Mod m : mods) {
                    String game = Names.get(m.tid.toUpperCase());
                    adapter.rows.add(new String[]{m.name,
                            m.tid + (game != null ? " · " + game : ""), "0"});
                }
                adapter.notifyDataSetChanged();
                if (error != null) status.setText("Erro: " + error);
                else status.setText(mods.isEmpty() ? "Nada instalado nesta pasta." : mods.size() + " mod(s)");
            });
        });
    }

    private void confirmDelete(final Mod m) {
        new AlertDialog.Builder(this)
                .setTitle("Apagar?")
                .setMessage(m.tid + " / " + m.name)
                .setPositiveButton("Apagar", (d, w) -> Util.POOL.execute(() -> {
                    String msg;
                    try {
                        SafStore.delete(this, SafStore.getTree(this), m.docId);
                        msg = "Apagado.";
                    } catch (Exception e) {
                        msg = "Erro: " + e.getMessage();
                    }
                    final String s = msg;
                    runOnUiThread(() -> {
                        Ui.toast(this, s);
                        refresh();
                    });
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
