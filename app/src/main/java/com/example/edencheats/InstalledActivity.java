package com.example.edencheats;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerenciador de Cheats & Mods Instalados:
 * Permite visualizar o status (Ativo/Desativado), alternar sem apagar,
 * inspecionar os códigos e desinstalar do emulador Eden.
 */
public class InstalledActivity extends BaseActivity {
    private static final class Mod {
        String tid;
        String name;
        String docId;
        String fileDocId;
        String fileName;
        boolean disabled;
    }

    private TextView status;
    private Ui.RowAdapter adapter;
    private final List<Mod> mods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);

        // Cabeçalho com botão voltar
        root.addView(Ui.headerCard(this, Lang.s(R.string.items_inst_title), Lang.s(R.string.items_inst_subtitle), Ui.MUTED), Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        Ui.styleList(list);
        list.setOnItemClickListener((pView, v, pos, id) -> {
            if (pos >= 0 && pos < mods.size()) {
                showModOptions(mods.get(pos));
            }
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
            status.setText(Lang.s(R.string.items_inst_pick_folder));
            return;
        }
        status.setText(Lang.s(R.string.items_inst_reading));
        Util.POOL.execute(() -> {
            final List<Mod> found = new ArrayList<>();
            String err = null;
            try {
                Uri tree = SafStore.getTree(this);
                Uri load = SafStore.loadDir(this, false);
                ContentResolver cr = getContentResolver();
                if (tree != null && load != null) {
                    for (SafStore.Entry t : SafStore.list(cr, tree, load)) {
                        if (!t.dir) continue;
                        Uri tUri = SafStore.doc(tree, t.docId);
                        for (SafStore.Entry m : SafStore.list(cr, tree, tUri)) {
                            if (!m.dir) continue;
                            Mod mod = new Mod();
                            mod.tid = t.name.toUpperCase(Locale.US);
                            mod.name = m.name;
                            mod.docId = m.docId;

                            // Verificar se dentro de m há uma pasta cheats/ com arquivos .txt ou .disabled
                            Uri mUri = SafStore.doc(tree, m.docId);
                            Uri cheatsFolder = SafStore.find(cr, tree, mUri, "cheats");
                            if (cheatsFolder != null) {
                                for (SafStore.Entry cf : SafStore.list(cr, tree, cheatsFolder)) {
                                    if (cf.dir) continue;
                                    String fn = cf.name.toLowerCase(Locale.US);
                                    if (fn.endsWith(".disabled") || fn.endsWith(".bak")) {
                                        mod.disabled = true;
                                        mod.fileDocId = cf.docId;
                                        mod.fileName = cf.name;
                                        break;
                                    } else if (fn.endsWith(".txt")) {
                                        mod.disabled = false;
                                        mod.fileDocId = cf.docId;
                                        mod.fileName = cf.name;
                                        break;
                                    }
                                }
                            }
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
                // Agrupa por jogo (ordem alfabética) para achar os cheats de cada jogo mais rápido.
                java.util.Collections.sort(mods, (a, b) -> {
                    int c = gameTitle(a).compareToIgnoreCase(gameTitle(b));
                    return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
                });
                adapter.rows.clear();
                for (Mod m : mods) {
                    String stateLabel = (m.disabled ? Lang.s(R.string.items_state_off) : Lang.s(R.string.items_state_on)) + " ";
                    // Título = nome do jogo; 4º item = Title ID, que faz a capa aparecer na linha.
                    String sub = stateLabel + m.name + " · " + m.tid;
                    adapter.rows.add(new String[]{gameTitle(m), sub, m.disabled ? "0" : "1", m.tid});
                }
                adapter.notifyDataSetChanged();
                if (error != null) status.setText(Lang.s(R.string.items_error, error));
                else status.setText(mods.isEmpty() ? Lang.s(R.string.items_inst_empty) : Lang.s(R.string.items_inst_count, mods.size()));
            });
        });
    }

    /** Nome do jogo para exibir (ou o Title ID quando o nome não é conhecido). */
    private static String gameTitle(Mod m) {
        String g = Names.get(m.tid);
        return (g != null && !g.isEmpty()) ? g : m.tid;
    }

    private void showModOptions(final Mod m) {
        String toggleLabel = m.disabled ? Lang.s(R.string.items_opt_enable) : Lang.s(R.string.items_opt_disable);
        String[] options = new String[]{
                Lang.s(R.string.items_opt_inspect),
                toggleLabel,
                Lang.s(R.string.items_opt_uninstall)
        };
        new AlertDialog.Builder(this)
                .setTitle(m.name)
                .setItems(options, (d, which) -> {
                    if (which == 0) viewCheatCode(m);
                    else if (which == 1) toggleMod(m);
                    else if (which == 2) confirmDelete(m);
                })
                .show();
    }

    private void viewCheatCode(final Mod m) {
        if (m.fileDocId == null) {
            Ui.toast(this, Lang.s(R.string.items_no_cheat_file));
            return;
        }
        Util.POOL.execute(() -> {
            String content;
            try {
                Uri tree = SafStore.getTree(this);
                Uri fileUri = SafStore.doc(tree, m.fileDocId);
                try (InputStream in = getContentResolver().openInputStream(fileUri)) {
                    if (in == null) throw new java.io.IOException(Lang.s(R.string.items_cant_read_file));
                    content = new String(Util.readAll(in), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                content = Lang.s(R.string.items_load_code_err, e.getMessage());
            }
            final String text = content;
            runOnUiThread(() -> {
                ScrollView sv = new ScrollView(this);
                int p = Ui.dp(this, 16);
                sv.setPadding(p, p, p, p);
                sv.setBackgroundColor(Ui.CARD);

                TextView tv = new TextView(this);
                tv.setText(text);
                tv.setTextColor(Ui.TEXT);
                tv.setTextSize(13);
                tv.setTypeface(Typeface.MONOSPACE);
                sv.addView(tv);

                new AlertDialog.Builder(this)
                        .setTitle(Lang.s(R.string.items_codes_title, m.name))
                        .setView(sv)
                        .setPositiveButton(Lang.s(R.string.items_copy_code), (d, w) -> {
                            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cm != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("Cheat", text));
                                Ui.toast(this, Lang.s(R.string.items_code_copied));
                            }
                        })
                        .setNegativeButton(Lang.s(R.string.items_close), null)
                        .show();
            });
        });
    }

    private void toggleMod(final Mod m) {
        if (m.fileDocId == null || m.fileName == null) {
            Ui.toast(this, Lang.s(R.string.items_no_toggle));
            return;
        }
        Util.POOL.execute(() -> {
            try {
                Uri tree = SafStore.getTree(this);
                Uri fileUri = SafStore.doc(tree, m.fileDocId);
                String newName;
                if (m.disabled) {
                    newName = m.fileName.replaceAll("(?i)\\.(disabled|bak)$", "");
                    if (!newName.endsWith(".txt")) newName += ".txt";
                } else {
                    newName = m.fileName + ".disabled";
                }
                SafStore.rename(getContentResolver(), fileUri, newName);
                runOnUiThread(() -> {
                    Ui.toast(this, m.disabled ? Lang.s(R.string.items_enabled_ok) : Lang.s(R.string.items_disabled_ok));
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, Lang.s(R.string.items_toggle_fail, e.getMessage())));
            }
        });
    }

    private void confirmDelete(final Mod m) {
        new AlertDialog.Builder(this)
                .setTitle(Lang.s(R.string.items_remove_title))
                .setMessage(Lang.s(R.string.items_delete_confirm, m.name, m.tid))
                .setPositiveButton(Lang.s(R.string.items_delete), (d, w) -> Util.POOL.execute(() -> {
                    String msg;
                    try {
                        SafStore.delete(this, SafStore.getTree(this), m.docId);
                        msg = Lang.s(R.string.items_deleted_ok);
                    } catch (Exception e) {
                        msg = Lang.s(R.string.items_delete_err, e.getMessage());
                    }
                    final String s = msg;
                    runOnUiThread(() -> {
                        Ui.toast(this, s);
                        refresh();
                    });
                }))
                .setNegativeButton(Lang.s(R.string.items_cancel), null)
                .show();
    }
}
