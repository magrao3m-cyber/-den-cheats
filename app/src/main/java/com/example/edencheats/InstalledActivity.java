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

        // Header Card
        LinearLayout headCard = new LinearLayout(this);
        headCard.setOrientation(LinearLayout.VERTICAL);
        headCard.setBackground(Ui.shape(this, Ui.CARD, 12, 1, Ui.BORDER));
        int p = Ui.dp(this, 14);
        headCard.setPadding(p, p, p, p);
        headCard.addView(Ui.text(this, "◈ CHEATS & MODS INSTALADOS", 19, Ui.TEXT, true));
        headCard.addView(Ui.text(this, "Toque em qualquer item para ver códigos, ativar/desativar ou desinstalar", 12, Ui.MUTED, false));
        root.addView(headCard, Ui.lp(this, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4));

        status = Ui.text(this, "", 12, Ui.MUTED, false);
        int sp = Ui.dp(this, 4);
        status.setPadding(sp, sp, sp, sp);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new Ui.RowAdapter(this);
        list.setAdapter(adapter);
        list.setDivider(null);
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
            status.setText("Escolha a pasta do Eden primeiro.");
            return;
        }
        status.setText("Lendo cheats instalados…");
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
                adapter.rows.clear();
                for (Mod m : mods) {
                    String game = Names.get(m.tid);
                    String stateLabel = m.disabled ? "[INATIVO] " : "[ATIVO] ";
                    String sub = m.tid + (game != null ? " · " + game : "") + (m.fileName != null ? " (" + m.fileName + ")" : "");
                    adapter.rows.add(new String[]{stateLabel + m.name, sub, m.disabled ? "0" : "1"});
                }
                adapter.notifyDataSetChanged();
                if (error != null) status.setText("Erro: " + error);
                else status.setText(mods.isEmpty() ? "Nenhum cheat ou mod instalado nesta pasta." : mods.size() + " mod(s)/cheat(s) encontrados no Eden");
            });
        });
    }

    private void showModOptions(final Mod m) {
        String toggleLabel = m.disabled ? "Ativar Cheat no Eden" : "Desativar Cheat (Sem Apagar)";
        String[] options = new String[]{
                "Inspecionar Códigos Hexadecimais",
                toggleLabel,
                "Desinstalar / Apagar do Eden"
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
            Ui.toast(this, "Arquivo de texto do cheat não encontrado.");
            return;
        }
        Util.POOL.execute(() -> {
            String content;
            try {
                Uri tree = SafStore.getTree(this);
                Uri fileUri = SafStore.doc(tree, m.fileDocId);
                try (InputStream in = getContentResolver().openInputStream(fileUri)) {
                    if (in == null) throw new java.io.IOException("Não foi possível ler o arquivo.");
                    content = new String(Util.readAll(in), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                content = "Erro ao carregar código: " + e.getMessage();
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
                        .setTitle("Códigos: " + m.name)
                        .setView(sv)
                        .setPositiveButton("Copiar Código", (d, w) -> {
                            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cm != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("Cheat", text));
                                Ui.toast(this, "Código copiado!");
                            }
                        })
                        .setNegativeButton("Fechar", null)
                        .show();
            });
        });
    }

    private void toggleMod(final Mod m) {
        if (m.fileDocId == null || m.fileName == null) {
            Ui.toast(this, "Arquivo não suporta alternância rápida.");
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
                    Ui.toast(this, m.disabled ? "⚡ Cheat ativado com sucesso!" : "⏸️ Cheat desativado.");
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, "Não foi possível alternar: " + e.getMessage()));
            }
        });
    }

    private void confirmDelete(final Mod m) {
        new AlertDialog.Builder(this)
                .setTitle("Remover do Eden?")
                .setMessage("Deseja apagar \"" + m.name + "\" do jogo " + m.tid + "?")
                .setPositiveButton("Apagar", (d, w) -> Util.POOL.execute(() -> {
                    String msg;
                    try {
                        SafStore.delete(this, SafStore.getTree(this), m.docId);
                        msg = "Cheat apagado com sucesso.";
                    } catch (Exception e) {
                        msg = "Erro ao apagar: " + e.getMessage();
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
